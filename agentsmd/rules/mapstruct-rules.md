# MapStruct Rules

> ℹ️ **APPLICABILITY:** This rule set applies ONLY if a MapStruct dependency (`org.mapstruct:mapstruct`, `org.mapstruct:mapstruct-processor`) is present in the **main** project dependencies or annotation processor configuration.

---

## Overview

The project uses a **hybrid mapping approach** combining MapStruct for compile-time safe DTO transformations with manual converters for complex business logic.

**Key Principles:**
- **MapStruct** for structural field mappings (compile-time safety, auto-generated code)
- **Manual converters** for business logic and complex transformations
- **Layered transformation**: External → Internal API → Service → Web Layer

**Configuration:**
- MapStruct version: see `build.gradle` or `pom.xml`
- Strict unmapped target policy: `ERROR` (compile fails if fields not mapped)
- Centralized mapper factory pattern (preferred, see below)

---

## Architecture Principles

### Layered Transformation Pattern

```
External DTOs (Feign clients)
    ↓
[*ClientService] - Error unwrapping, minimal transformation
    ↓
Internal API DTOs (internal-api modules)
    ↓
[Converters / *ServiceConverter] - Business transformations, enrichment
    ↓
Web Layer DTOs (web.api.* packages)
    ↓
[DataWrappers] - Response formatting (single/list/page)
    ↓
REST API Response
```

### Separation of Concerns

| Layer | Responsibility | Tool |
|-------|---------------|------|
| **Integration** | Unwrap `Result<T>`, error handling | `*ClientService` |
| **Structural mapping** | Field-to-field DTO conversion | MapStruct |
| **Business logic** | Multi-step transformations, validations | Manual converters |
| **Response formatting** | API response wrapping | `DataWrappers` |

---

## Mapper Organization (Preferred Pattern)

### Preferred: Centralized Mapper Factory

Consolidating MapStruct mapper interfaces into a single `MapperFactory` class (or a small number of cohesive factory classes) is the **PREFERRED** organization pattern. Access static mapper instances from this central location.

**Benefits:**
- Single discovery point — developers and agents find all mappings in one place
- Reduced duplication across the codebase
- Consistent mapper lifecycle (all static, all dependency-free)
- Easier auditing of mapping logic

**Example:**

```java
@Mapper
public interface UserMapper {
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);
    
    UserDto toDto(UserEntity entity);
    UserEntity toEntity(UserDto dto);
}

@Mapper
public interface OrderMapper {
    OrderMapper INSTANCE = Mappers.getMapper(OrderMapper.class);
    
    OrderDto toDto(OrderEntity entity);
    OrderEntity toEntity(OrderDto dto);
}

// Centralized factory for discovery
public final class MapperFactory {
    private MapperFactory() {
        // Prevent instantiation
    }
    
    public static UserMapper userMapper() {
        return UserMapper.INSTANCE;
    }
    
    public static OrderMapper orderMapper() {
        return OrderMapper.INSTANCE;
    }
}
```

### When Decentralization is Acceptable

Per-domain or per-module mapper organization is permitted when:
- The project already uses per-domain mappers and centralizing would be a large, risky refactor
- A mapper has injected dependencies (requires `componentModel = "spring"`) and naturally lives near its collaborators

**Guidance:** For new projects or greenfield modules, default to the centralized factory. For existing mature projects, do not force a reorganization — follow the established convention but prefer consolidation when adding new mappers.

---

## Static Mapper Access vs Spring Component

### Rule

- A MapStruct mapper with **NO injected dependencies** MAY be implemented as a static utility class accessed via `Mappers.getMapper()` (or a `public static final` field). Constructor injection is allowed but **NOT required** for dependency-free mappers.
- A mapper that requires a runtime collaborator (`@Context` backed by a bean, `@AfterMapping` calling a service) **MUST** be a Spring component (`componentModel = "spring"`) with constructor injection.

### Rationale

Pure structural mapping is a stateless function — DI adds no value. But once a mapper has collaborators, it must participate in the DI graph for testability and lifecycle correctness.

### Boundary

This is the **ONLY** exemption to the constructor-injection rule in [`code-style-rules.md`](./code-style-rules.md). All other classes remain subject to mandatory constructor injection.

### Example: Static Mapper (No Dependencies)

```java
@Mapper
public interface ProductMapper {
    ProductMapper INSTANCE = Mappers.getMapper(ProductMapper.class);
    
    ProductDto toDto(ProductEntity entity);
}

// Usage
ProductDto dto = MapperFactory.productMapper().toDto(entity);
```

### Example: Spring Component Mapper (With Dependencies)

```java
@Mapper(componentModel = "spring")
@Component
public interface EnrichedUserMapper {
    
    @Mapping(target = "fullName", source = ".", qualifiedByName = "buildFullName")
    UserDto toDto(UserEntity entity);
    
    @AfterMapping
    default void enrichWithExternalData(UserEntity entity, @MappingTarget UserDto dto) {
        // This requires an injected service - must be a Spring bean
        String externalInfo = externalService.getInfo(entity.getId());
        dto.setExternalInfo(externalInfo);
    }
}
```

---

## MapStruct Approach

### Configuration

- **Mapper location**: Centralized in `MapperFactory` class (preferred) or per-domain mappers
- **Instance access**: Static via `Mappers.getMapper()` for dependency-free mappers; Spring DI for mappers with collaborators
- **Validation**: Compile-time error on unmapped fields
- **Annotation processing**: Configured in `build.gradle` or `pom.xml`

### Field Mapping Strategy

#### Priority 1: Auto-Matching (Preferred)

When field names and types match exactly, **no annotations needed**:

```java
@Mapper
interface MyMapper {
    // MapStruct auto-matches: id→id, name→name, email→email
    TargetDto toDto(SourceEntity src);
}
```

**When to use**: 80%+ of fields have identical names and compatible types.

---

#### Priority 2: Explicit Mapping (When Names Differ)

Use `@Mapping` annotations only for fields that differ:

```java
@Mapper
interface MyMapper {
    // Auto-matches: id, firstName, lastName
    // Explicit: fullName (computed), status (renamed)
    @Mapping(target = "fullName", source = ".", qualifiedByName = "buildFullName")
    @Mapping(target = "status", source = "statusCode")
    TargetDto toDto(SourceEntity src);
}
```

**Benefits**:
- Clear which fields need special handling
- Auto-matched fields don't clutter the code
- Compile-time safety preserved

---

#### Priority 3: Custom Methods (Complex Logic)

Use `@Named` for transformations beyond simple field access:

```java
@Mapper
interface MyMapper {
    @Mapping(target = "fullName", source = ".", qualifiedByName = "buildFullName")
    TargetDto toDto(SourceEntity src);
    
    @Named("buildFullName")
    default String buildFullName(SourceEntity src) {
        return src.getFirstName() + " " + src.getLastName();
    }
}
```

---

### Common MapStruct Patterns

| Pattern | Annotation | Use Case |
|---------|-----------|----------|
| **Field mapping** | `@Mapping(target = "x", source = "y")` | Different field names |
| **Custom method** | `@Named("methodName")` | Complex transformations |
| **Post-processing** | `@AfterMapping` | Enrichment after mapping |
| **Context parameter** | `@Context` | Runtime data (snapshots, caches) |
| **Polymorphic mapping** | `@SubclassMapping` | Inheritance hierarchies |
| **Null handling** | `defaultValue = "..."` | Default values for nulls |
| **Expression** | `expression = "java(...)"` | Inline null or constants |

---

### When to Use MapStruct

✅ **Use MapStruct for:**
- Direct field mappings between DTOs
- Enum name conversions
- Simple type transformations (String → UUID)
- Nested object mapping
- Collections and lists

❌ **Use manual converters for:**
- Business logic validation
- Multi-step transformations
- External service calls
- Conditional logic based on runtime data
- Complex null handling with business rules

---

## Manual Converters

### Spring Component Pattern

Converters implemented as `@Component` or `@Service` beans with constructor injection:

```java
@Component
@RequiredArgsConstructor
public class MyConverter {
    private final ExternalService externalService;
    
    public TargetDto convert(SourceDto source) {
        // Complex business logic
        // Multi-step transformations
        // External service calls
        return new TargetDto(...);
    }
}
```

**When to use:**
- Business logic required during transformation
- Data enrichment from external sources
- Runtime conditional logic
- Multi-step aggregations

---

### Common Manual Patterns

#### Constructor-based mapping (simple cases)

```java
public TargetDto convert(SourceDto source) {
    return new TargetDto(
        source.getField1(),
        source.getField2(),
        source.getField3()
    );
}
```

**⚠️ Caution**: Only use when:
- All field names match exactly
- Field order is guaranteed stable
- No null handling needed
- Few fields (< 5-7)

**Risk**: Positional parameters are fragile to constructor changes.

---

#### Builder pattern (complex DTOs)

```java
public TargetDto convert(SourceDto source) {
    return TargetDto.builder()
        .field1(source.getField1())
        .field2(source.getField2())
        .field3(computeField3(source))
        .build();
}
```

**Benefits**:
- Named parameters (clear intent)
- Immutable result
- Safe to field order changes

---

#### Stream transformations (collections)

```java
public List<TargetDto> convert(List<SourceDto> sources) {
    return sources.stream()
        .map(this::convertSingle)
        .collect(Collectors.toList());
}
```

---

#### Optional chaining (null safety)

```java
public String getFullName(SourceDto source) {
    return Optional.ofNullable(source.getName())
        .map(Name::getFullName)
        .orElse("Unknown");
}
```

---

#### Switch expressions (enum/type routing)

```java
public TargetType convert(SourceType source) {
    return switch (source) {
        case TYPE_A -> TargetType.A;
        case TYPE_B -> TargetType.B;
        case TYPE_C -> TargetType.C;
    };
}
```

---

## Design Decisions & Rationale

### Why Hybrid Approach?

| Aspect | MapStruct | Manual |
|--------|-----------|--------|
| **Compile-time safety** | ✅ Yes | ❌ No |
| **Auto-generated code** | ✅ Yes | ❌ No |
| **Business logic** | ❌ Limited | ✅ Full control |
| **External calls** | ❌ No | ✅ Yes |
| **Complex transformations** | ⚠️ Verbose | ✅ Clean |
| **Maintenance** | ✅ Auto-updates | ⚠️ Manual |

**Decision**: Use each tool for its strengths.

---

### Why Centralized Mappers?

**Benefits:**
- Single source of truth for mapping logic
- Easy discovery and reuse
- Consistent patterns across codebase
- Reduced duplication

**Trade-off**: Large file requires organization (use regions/comments).

---

### Why Static Instances for Dependency-Free Mappers?

**Benefits:**
- No Spring DI overhead for simple mappings
- Test-friendly (no context required)
- Clear separation from business logic
- Can be used in utility classes

**Trade-off**: Cannot inject dependencies (use manual converters for that).

**Note**: This is the only exemption to the constructor-injection rule. See [`code-style-rules.md`](./code-style-rules.md) for the general DI requirement.

---

### Why NOT Constructor-Only Mapping?

While constructor calls are more concise, they have critical weaknesses:

| Issue | Constructor Approach | MapStruct Approach |
|-------|---------------------|-------------------|
| **Field reordering** | ❌ Breaks silently | ✅ Safe |
| **Missing fields** | ⚠️ Compile error only if required | ✅ Compile error always |
| **Positional confusion** | ❌ Easy to swap params | ✅ Named mapping |
| **Null handling** | ❌ Manual | ✅ Auto-generated |
| **Field renaming** | ❌ Manual updates needed | ✅ Auto-updates |

**Recommendation**: Use MapStruct even for simple 1:1 mappings to preserve compile-time safety and maintainability.

**Exception**: Manual constructor calls acceptable for:
- Simple DTOs with < 5 fields
- All fields auto-matched by name
- No null handling needed
- Stable constructor signature

---

## Testing Patterns

### MapStruct Testing

```java
class MyMapperTest {
    private static final MyMapper MAPPER = Mappers.getMapper(MyMapper.class);
    
    @Test
    void testMapping() {
        SourceDto source = new SourceDto("id", "name");
        
        TargetDto result = MAPPER.toDto(source);
        
        assertThat(result.getId()).isEqualTo("id");
        assertThat(result.getName()).isEqualTo("name");
    }
}
```

**Benefits:**
- No Spring context required
- Fast unit tests
- Focus on field mapping verification

**Note**: For general testing patterns, see [`testing-approach-rules.md`](./testing-approach-rules.md).

---

### Manual Converter Testing

```java
@ExtendWith(MockitoExtension.class)
class MyConverterTest {
    @Mock
    private ExternalService externalService;
    
    @InjectMocks
    private MyConverter converter;
    
    @Test
    void testConvert_withBusinessLogic() {
        when(externalService.getData()).thenReturn(...);
        
        TargetDto result = converter.convert(source);
        
        assertThat(result.getField()).isEqualTo(...);
        verify(externalService).getData();
    }
}
```

**Note**: Follows standard Mockito testing patterns from [`testing-approach-rules.md`](./testing-approach-rules.md).

---

## Adding New Mappings

### Decision Tree

```
Need to map DTO A → DTO B?
    │
    ├─→ Simple field mappings (80%+ match)?
    │   └─→ Use MapStruct in MapperFactory
    │       ├─→ Auto-match if names identical
    │       └─→ Add @Mapping only for differences
    │
    └─→ Complex business logic needed?
        ├─→ External service calls?
        │   └─→ Manual @Component converter
        │
        ├─→ Multi-step transformations?
        │   └─→ Manual @Component converter
        │
        └─→ Runtime conditional logic?
            └─→ Manual @Component converter
```

---

### Checklist for New DTO Mappings

#### For MapStruct Mappers

- [ ] Add method to appropriate mapper interface in `MapperFactory` (or create new mapper if needed)
- [ ] Prefer auto-matching (no annotations) when field names match
- [ ] Use `@Mapping` only for fields that differ
- [ ] Add `@Named` method if custom logic needed
- [ ] Verify no unmapped fields (build will fail otherwise)
- [ ] Add test case with field mapping verification

#### For Manual Converters

- [ ] Create new `@Component` class or extend existing
- [ ] Inject required services for enrichment
- [ ] Document transformation logic with JavaDoc
- [ ] Add unit tests with business scenarios
- [ ] Mock external service dependencies

#### For New Integration Clients

- [ ] MapStruct for simple DTO transformation
- [ ] Manual converter if business logic required
- [ ] Follow `*ClientService` wrapper pattern (see [`integration-rules.md`](./integration-rules.md))
- [ ] Handle `Result<T>` wrapper errors appropriately (see [`integration-rules.md`](./integration-rules.md))

---

## Related Documentation

- [`integration-rules.md`](./integration-rules.md) - Feign client wrappers and HTTP client patterns
- [`code-style-rules.md`](./code-style-rules.md) - DTO naming, Lombok usage, constructor injection rule
- [`testing-approach-rules.md`](./testing-approach-rules.md) - Test base classes, AssertJ patterns, Mockito verification

---

## Quick Reference

### Common Patterns Cheat Sheet

```java
// 1. Auto-matching (preferred)
TargetDto toDto(SourceEntity src);

// 2. Explicit field mapping
@Mapping(target = "fullName", source = "name")
TargetDto toDto(SourceEntity src);

// 3. Custom method
@Mapping(target = "computed", source = ".", qualifiedByName = "compute")
TargetDto toDto(SourceEntity src);

@Named("compute")
default String compute(SourceEntity src) { ... }

// 4. Context parameter
TargetDto toDto(SourceEntity src, @Context Snapshot snapshot);

// 5. Post-processing
@AfterMapping
default void afterMapping(SourceEntity src, @MappingTarget TargetDto target) { ... }

// 6. Manual converter
@Component
public class MyConverter {
    public TargetDto convert(SourceEntity src) {
        return new TargetDto(src.getField1(), src.getField2());
    }
}

// 7. Static mapper instance (dependency-free only)
@Mapper
interface MyMapper {
    MyMapper INSTANCE = Mappers.getMapper(MyMapper.class);
    TargetDto toDto(SourceEntity src);
}
```
