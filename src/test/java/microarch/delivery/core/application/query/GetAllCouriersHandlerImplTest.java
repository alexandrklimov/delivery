package microarch.delivery.core.application.query;

import lombok.val;
import microarch.delivery.Constants;
import microarch.delivery.core.domain.model.Location;
import microarch.delivery.core.domain.model.courier.Courier;
import microarch.delivery.core.ports.CourierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAllCouriersHandlerImplTest {

    @Mock
    private CourierRepository courierRepository;

    private GetAllCouriersHandlerImpl handler;

    @BeforeEach
    void setUp() {
        handler = new GetAllCouriersHandlerImpl(courierRepository);
    }

    /* ---------- argument validation ---------- */

    @Test
    void testNullQueryFails() {
        val result = handler.handle(null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo(Constants.ERR_CODE_OBJ_IS_NULL);
        verifyNoInteractions(courierRepository);
    }

    /* ---------- happy path ---------- */

    @Test
    void testEmptyListSuccess() {
        when(courierRepository.getAll()).thenReturn(List.of());

        val result = handler.handle(new GetAllCouriersQuery());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEmpty();
    }

    @Test
    void testMapsAllCouriersToDto() {
        val first = Courier.create("Ivan", Location.create(1, 2).getValue()).getValue();
        val second = Courier.create("Petr", Location.create(3, 4).getValue()).getValue();
        when(courierRepository.getAll()).thenReturn(List.of(first, second));

        val result = handler.handle(new GetAllCouriersQuery());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).containsExactly(
                new CourierDTO(first.getId(), "Ivan", Location.create(1, 2).getValue()),
                new CourierDTO(second.getId(), "Petr", Location.create(3, 4).getValue()));
    }
}
