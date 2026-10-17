# Integration Rules

## HTTP Client Pattern

**Important**: Any HTTP client (Feign, RestClient, WebClient) should adhere to the rule `Single client interface per single API-interface`. Extending an existing client interface is prohibited. Each external API gets its own dedicated client interface.

```java
@FeignClient(name = "user-service-client", url = "${app.user-service.url}")
public interface UserClient {
    @GetMapping("/api/users/{userId}")
    Result<UserDTO> findUserById(@PathVariable UUID userId);
}
```

## HTTP Client Method Annotation Rule

**CRITICAL**: When an HTTP client extends a base API interface, ALL methods MUST have proper Spring HTTP method annotations (`@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, `@PatchMapping`).

**Base Interface Methods**: Base API interfaces define method signatures WITHOUT Spring HTTP annotations.

**Client Responsibility**: When extending a base interface, the client MUST ensure every method has a proper HTTP annotation using one of these approaches:

**Approach 1: Override with annotation (RECOMMENDED)**
```java
@FeignClient(name = "client-name", url = "${app.service.url}")
public interface ServiceClient extends BaseServiceApi {
    
    @Override
    @PostMapping(value = "/endpoint", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseWrapper<ResponseDTO> createResource(@RequestBody CreateRequestDTO request);
}
```

**Approach 2: Annotate base interface method (if you own the base interface)**
```java
// In base interface (e.g., BaseServiceApi.java)
public interface BaseServiceApi {
    
    @PostMapping(value = "/endpoint", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseWrapper<ResponseDTO> createResource(CreateRequestDTO request);
}
```

**When Base Interface is Updated:**

If a new method is added to a base API interface:
1. ✅ **Annotate the new method in the base interface** with appropriate Spring HTTP annotation, OR
2. ✅ **Override the method in the client** with the annotation

**Failure to add annotations will result in runtime errors** as the framework cannot determine the HTTP method and path for unannotated methods.

**Examples:**

✅ **Correct** - Override with annotation:
```java
@Override
@PostMapping(value = "/resource", consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
ResponseWrapper<DTO> createResource(CreateRequestDTO request);
```

✅ **Correct** - Method with GET annotation:
```java
@GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
ResponseWrapper<DTO> getResource(@PathVariable("id") UUID id);
```

## HTTP Client Testing Strategy

**PRINCIPLE**: HTTP clients MUST be mocked in unit tests (95-99% of cases).

**Rationale**: HTTP clients perform calls to external systems not under our control. Unit tests should verify business logic, not network integrations.

**Testing Pattern**:
```java
@ExtendWith(MockitoExtension.class)
class MyServiceTest {
    @Mock
    private MyClient client;  // Mock the HTTP client
    
    @InjectMocks
    private MyClientService service;  // Test the service wrapper
    
    @Test
    void shouldHandleSuccess() {
        // Arrange
        when(client.getData()).thenReturn(ResponseWrapper.single(expectedDto));
        
        // Act
        val result = service.getData();
        
        // Assert
        assertThat(result).isEqualTo(expectedDto);
        verify(client).getData();  // Verify client was called
    }
}
```

**What to test**:
- ✅ `*ClientService` wrapper logic (error handling, transformations, logging)
- ✅ Business logic that uses client service responses
- ❌ NOT HTTP client calls directly (that's integration testing)

**Integration tests**: Use `@MockBean` or testcontainers only when testing full integration scenarios.

## Result Object Error Handling

**CRITICAL**: Any external API communication that returns a unified wrapper object (e.g., `Result<T>`, `ResponseWrapper<T>`) MUST be explicitly validated for errors before processing data.

### Responsibilities
* **Error Unwrapping:** Always check `Result.isError()` (or equivalent). If true, throw an exception immediately.
* **No Swallowing:** Do NOT return default values, empty lists, or `null` on failure unless explicitly requested by the business task.
* **Message Transformation:** Convert raw infrastructure errors into meaningful business messages within the exception.

### Rationale
* **Data Consistency:** Swallowing errors hides failures, leading to corrupt or inconsistent application states.
* **Observability:** Immediate exceptions ensure that Sentry, OpenTelemetry, or APM tools trigger alerts.
* **Debugging:** Prevents lost stack traces, making production root-cause analysis possible.

### Code Examples

**❌ INCORRECT (Anti-pattern)**
```java
// Swallowing errors or processing raw data without validation
Result<UserDto> response = restClient.get("/users/1", Result.class);

if (response.isError()) {
    return null; // ❌ CRITICAL: Returning null masks the failure
}
return response.getData();
```

**✅ CORRECT**
```java
Result<UserDto> response = restClient.get("/users/1", Result.class);

if (response.isError()) {
    log.error("External API call failed. Errors: {}", response.getErrors());
    throw new IntegrationException("Failed to fetch user data due to external service error");
}

return response.getData(); // Clean data extraction after verification
```

## HTTP Client Wrapper Isolation

**CRITICAL**: HTTP client interfaces MUST NOT be injected or called directly inside controllers or business services (`@Service`). Every HTTP client MUST have a dedicated wrapper service.

### Naming Convention
* **Client Interface:** `*Client` or `*FeignClient` (e.g., `UserClient`, `UserFeignClient`)
* **Wrapper Class:** `*ClientService`, `*Wrapper`, or `*Adapter` (e.g., `UserClientService`, `UserClientWrapper`)

### Responsibilities of the Wrapper
* **Infrastructure Isolation:** Hide the HTTP client dependency from the core business logic.
* **Data Mapping:** Convert transport DTOs from the client into internal business/domain DTOs.
* **Centralized Logging:** Log requests, responses, and errors consistently with method names and context identifiers.
* **Error Handling Alignment:** Apply the "Result Object Error Handling" rule inside this wrapper.

### Benefits
1. **Separation of Concerns:** External API contract changes affect only the wrapper layer, not business logic.
2. **Testability:** Business services can easily mock the wrapper interface without dealing with HTTP client mocking.

### Code Examples

**❌ INCORRECT (Anti-pattern)**
```java
@Service
public class OrderService {
    @Autowired 
    private UserClient userClient; // ❌ Direct infrastructure dependency inside business service

    public void processOrder(OrderRequest req) {
        Result<UserDto> res = userClient.getUser(req.getUserId()); // ❌ Direct call
        // ...
    }
}
```

**✅ CORRECT**
```java
// 1. Isolated Infrastructure Interface
@FeignClient(name = "user-service")
public interface UserClient {
    Result<UserResponse> getUser(UUID id);
}

// 2. Dedicated Wrapper
@Component
@Slf4j
public class UserClientService {
    @Autowired 
    private UserClient userClient;

    public UserDomainDto getUserById(UUID id) {
        log.info("Executing external user fetch for id={}", id);
        Result<UserResponse> result = userClient.getUser(id);
        
        if (result.isError()) {
            log.error("User service error for id={}. Errors: {}", id, result.getErrors());
            throw new IntegrationException("User profile is temporarily unavailable");
        }
        
        return convertToDomain(result.getData()); // Maps to internal domain object
    }
}

// 3. Clean Business Service
@Service
public class OrderService {
    @Autowired 
    private UserClientService userGateway; // ✅ Depends on wrapper, not the client directly

    public void processOrder(OrderRequest req) {
        UserDomainDto user = userGateway.getUserById(req.getUserId()); // ✅ Clean business DTO
        // ...
    }
}
```

## Cache Pattern

When using in-memory caches (Caffeine, Guava, or similar), wrap the cache in a service with typed access methods:

```java
@Service
public class PermissionsCacheService {
    private final Cache<UUID, Collection<String>> cache;
    
    public Collection<String> get(UUID entityId, String contextKey) {
        return cache.getIfPresent(createKey(entityId, contextKey));
    }
    
    public void put(UUID entityId, String contextKey, Collection<String> permissions) {
        cache.put(createKey(entityId, contextKey), permissions);
    }
}
```
