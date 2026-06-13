package com.example.thestar1.repository;

import com.example.thestar1.entity.OrderListVO;
import com.example.thestar1.entity.StayRecordVO;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StayRecordRepository extends JpaRepository<StayRecordVO, Integer> {


    int countByOrderListvo(OrderListVO orderListVO);


    StayRecordVO findByRoomIdAndCheckOutTimeIsNull(Integer roomId);

    //找出屬於這張訂單的訂單明細的所有住宿紀錄不管有無退房
    //COUNT * FROM STAY_RECORD s JOIN ROOM_ORDER_LIST ol ON s.ORDER_LIST_ID = ol.ORDER_LIST_ID
    //WHERE ol.ORDER_ID = :orderId
    int countByOrderListvoOrdervoOrderId(Integer orderId);

    //找出屬於這張訂單有退房的住宿紀錄
    int countByOrderListvoOrdervoOrderIdAndCheckOutTimeIsNull(Integer orderId);


}
