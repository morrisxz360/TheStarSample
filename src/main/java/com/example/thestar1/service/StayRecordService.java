package com.example.thestar1.service;

import com.example.thestar1.dto.CheckInDTO;
import com.example.thestar1.entity.OrderListVO;
import com.example.thestar1.entity.OrderVO;
import com.example.thestar1.entity.RoomVO;
import com.example.thestar1.entity.StayRecordVO;
import com.example.thestar1.repository.OrderListRepository;
import com.example.thestar1.repository.RoomRepository;
import com.example.thestar1.repository.StayRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class StayRecordService {

    private final RoomRepository roomRepository;
    private final StayRecordRepository stayRecordRepository;
    private final OrderListRepository orderListRepository;
    private final OrderService orderService;

    @Autowired
    public StayRecordService(RoomRepository roomRepository, StayRecordRepository stayRecordRepository,
                             OrderListRepository orderListRepository, OrderService orderService) {
        this.roomRepository = roomRepository;
        this.stayRecordRepository = stayRecordRepository;
        this.orderListRepository = orderListRepository;
        this.orderService = orderService;
    }

    @Transactional
    public void checkIn(Integer employeeId, CheckInDTO dto) {

        Integer orderListId = dto.getOrderListId();
        OrderListVO orderList = orderListRepository.findById(orderListId)
                .orElseThrow(() -> new IllegalArgumentException("明細不存在"));

        OrderVO order = orderList.getOrdervo();
        if (order.getOrderStatus() != 1) {
            throw new IllegalStateException("訂單非以付款，無法checkin");
        }


        int fullyBooked = stayRecordRepository.countByOrderListvo(orderList);
        if (fullyBooked >= orderList.getQuantity()) {
            throw new IllegalStateException("此明細以配滿房間數");
        }

        RoomVO room = roomRepository.findByRoomId(dto.getRoomId());
        if (room == null) {
            throw new IllegalArgumentException("房間不存在");
        } else if (!room.getRoomTypeId().equals(orderList.getRoomTypeId())) {
            throw new IllegalStateException("此房型不正確");
        } else if (room.getRoomStatus() == 1) {
            throw new IllegalStateException("此房間以有人入住");
        } else if (room.getRoomSwitchStatus() == false) {
            throw new IllegalStateException("此房間停用中");
        } else {
            room.setRoomStatus((byte) 1);
        }


        StayRecordVO stay = new StayRecordVO();
        stay.setCheckInEmployeeId(employeeId);
        stay.setRoomId(dto.getRoomId());
        stay.setOrderListvo(orderList);
        stay.setStayCustomer(dto.getStayCustomer());
        stayRecordRepository.save(stay);

    }


    @Transactional
    public StayRecordVO checkOut(Integer roomId, Integer employeeId) {

        //先用客人房號找出住宿明細
        StayRecordVO stay = stayRecordRepository.findByRoomIdAndCheckOutTimeIsNull(roomId);
        if (stay == null) {
            throw new IllegalStateException("查無房號");
        }
        stay.setCheckOutTime(LocalDateTime.now());
        stay.setCheckOutEmployeeId(employeeId);
        //用房號建立一個room物件
        RoomVO room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalStateException("房間資料不存在"));
        if (room.getRoomStatus() == 0) {
            throw new IllegalStateException("房間未入住");
        }
        //確認後退房
        room.setRoomStatus((byte) 0);

        roomRepository.save(room);
        //確認未退房的房間為零以及總訂房間數跟住宿紀錄總量是相同的確保該住的都有住該退的都退了
        int orderId = stay.getOrderListvo().getOrdervo().getOrderId();
        int notCheckOutRooms = stayRecordRepository.countByOrderListvoOrdervoOrderIdAndCheckOutTimeIsNull(orderId);
        int totalBookedRooms = orderListRepository.sumQtyByOrderId(orderId);
        int totalStayRecord = stayRecordRepository.countByOrderListvoOrdervoOrderId(orderId);

        if (notCheckOutRooms == 0 && totalBookedRooms == totalStayRecord) {
            orderService.completeOrder(orderId);
        }
        stayRecordRepository.save(stay);
        return stay;
    }

}
