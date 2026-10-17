package microarch.delivery.core.application.command;

import lombok.val;
import microarch.delivery.Constants;
import microarch.delivery.core.domain.model.Location;
import microarch.delivery.core.domain.model.courier.Courier;
import microarch.delivery.core.ports.CourierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateCourierHandlerImplTest {

    @Mock
    private CourierRepository courierRepository;

    private CreateCourierHandlerImpl handler;

    @BeforeEach
    void setUp() {
        handler = new CreateCourierHandlerImpl(courierRepository);
    }

    /* ---------- argument validation ---------- */

    @Test
    void testNullCommandFails() {
        val result = handler.handle(null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo(Constants.ERR_CODE_OBJ_IS_NULL);
        verifyNoInteractions(courierRepository);
    }

    /* ---------- happy path ---------- */

    @Test
    void testSuccessCreatesCourier() {
        val persisted = Courier.create("Ivan", Location.create(1, 1).getValue()).getValue();
        when(courierRepository.create(any(Courier.class))).thenReturn(persisted);

        val result = handler.handle(new CreateCourierCommand("Ivan"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getName()).isEqualTo("Ivan");

        val captor = ArgumentCaptor.forClass(Courier.class);
        verify(courierRepository).create(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Ivan");
        assertThat(captor.getValue().getLocation()).isEqualTo(Location.create(1, 1).getValue());
    }
}
