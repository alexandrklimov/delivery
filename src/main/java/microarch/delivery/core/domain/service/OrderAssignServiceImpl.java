package microarch.delivery.core.domain.service;

import jakarta.validation.constraints.NotNull;
import libs.errs.Error;
import libs.errs.Result;
import lombok.val;
import microarch.delivery.Constants;
import microarch.delivery.core.domain.model.courier.Courier;
import microarch.delivery.core.domain.model.order.Order;
import microarch.delivery.core.domain.model.order.OrderStatus;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;

@Service
public class OrderAssignServiceImpl implements OrderAssignService {

    @Override
    public Result<Courier, Error> assign(@NotNull final Order order, @NotNull final Collection<Courier> couriers) {
        if (order == null)
            return Result.failure(Error.of(Constants.ERR_CODE_OBJ_IS_NULL, "order must not be null"));

        if (couriers == null)
            return Result.failure(Error.of(Constants.ERR_CODE_OBJ_IS_NULL, "couriers must not be null"));

        if (order.getStatus() != OrderStatus.Created)
            return Result.failure(Error.of("assignsrvc.order.wrong.status", "Created status is allowed only"));

        val nearestCourierEntryOpt = couriers.stream()
                .filter(c -> c.checkCanAssignOneMore(order.getVolume()).isSuccess()) // filter out overloaded couriers
                .map(c -> Map.entry( // build {Courier, Distance.value} entry
                        c, c.getLocation().computeDistance(order.getLocation()).getValueOrThrow()))
                .min(Map.Entry.comparingByValue());

        if (nearestCourierEntryOpt.isPresent()) {
            val nearestCourier = nearestCourierEntryOpt.orElseThrow().getKey();

            val assignResult = nearestCourier.assignOrder(order);
            if (assignResult.isFailure())
                return assignResult;

            val markAssignedRes = order.markAsAssigned();
            if (markAssignedRes.isFailure())
                return Result.failure(markAssignedRes.getError());

            return Result.success(nearestCourier);
        } else {
            return Result
                    .failure(Error.of("assignsrvc.no.available.courier", "There are no couriers available to assign."));
        }
    }

}
