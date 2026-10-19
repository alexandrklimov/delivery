package microarch.delivery.core.application.command;

import lombok.val;
import microarch.delivery.Constants;
import microarch.delivery.core.domain.model.Location;
import microarch.delivery.core.domain.model.courier.Courier;
import microarch.delivery.core.ports.CourierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoveCourierHandlerImplTest {

    @Mock
    private CourierRepository courierRepository;

    private MoveCourierHandlerImpl handler;

    @BeforeEach
    void setUp() {
        handler = new MoveCourierHandlerImpl(courierRepository);
    }

    private static Courier courierAt(final int x, final int y) {
        return Courier.create("Ivan", Location.create(x, y).getValue()).getValue();
    }

    /* ---------- argument validation ---------- */

    @Test
    void testNullCommandFails() {
        val result = handler.handle(null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo(Constants.ERR_CODE_OBJ_IS_NULL);
        verify(courierRepository, never()).update(any(Courier.class));
    }

    @Test
    void testCourierNotFoundFails() {
        val courierId = UUID.randomUUID();
        when(courierRepository.findById(courierId)).thenReturn(Optional.empty());

        val result = handler.handle(new MoveCourierCommand(courierId, Location.create(5, 5).getValue()));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("courier.not.found");
        verify(courierRepository, never()).update(any(Courier.class));
    }

    /* ---------- moves ---------- */

    /**
     * Verifies that the handler persists the courier at whatever valid location the command carries — one-step and
     * multi-step targets are treated the same way.
     */
    @ParameterizedTest
    @MethodSource
    void testMoveApplied(final int fromX, final int fromY, final int toX, final int toY) {
        val courier = courierAt(fromX, fromY);
        when(courierRepository.findById(courier.getId())).thenReturn(Optional.of(courier));
        when(courierRepository.update(any(Courier.class))).thenReturn(courier);

        val result = handler.handle(new MoveCourierCommand(courier.getId(), Location.create(toX, toY).getValue()));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getLocation()).isEqualTo(Location.create(toX, toY).getValue());
        verify(courierRepository).update(courier);
    }

    static Stream<Arguments> testMoveApplied() {
        // one step in each direction, two steps horizontally, two steps vertically, L-path, corner to corner
        return Stream.of(Arguments.of(5, 5, 6, 5), Arguments.of(5, 5, 4, 5), Arguments.of(5, 5, 5, 6),
                Arguments.of(5, 5, 5, 4), Arguments.of(5, 5, 8, 5), Arguments.of(5, 5, 5, 2), Arguments.of(2, 2, 6, 9),
                Arguments.of(1, 1, 10, 10));
    }

    /* ---------- no-op move ---------- */

    @Test
    void testMoveToCurrentLocationIsSuccessWithoutChanges() {
        val courier = courierAt(5, 5);
        when(courierRepository.findById(courier.getId())).thenReturn(Optional.of(courier));
        when(courierRepository.update(any(Courier.class))).thenReturn(courier);

        val result = handler.handle(new MoveCourierCommand(courier.getId(), Location.create(5, 5).getValue()));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getLocation()).isEqualTo(Location.create(5, 5).getValue());
        verify(courierRepository).update(courier);
    }
}
