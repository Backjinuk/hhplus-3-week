package com.example.hh3week.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.hh3week.application.port.out.ReservationSeatRepositoryPort;
import com.example.hh3week.common.config.CustomException;
import com.example.hh3week.domain.reservation.entity.QReservationSeat;
import com.example.hh3week.domain.reservation.entity.QReservationSeatDetail;
import com.example.hh3week.domain.reservation.entity.ReservationSeat;
import com.example.hh3week.domain.reservation.entity.ReservationSeatDetail;
import com.example.hh3week.domain.reservation.entity.ReservationStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

@Repository
public class ReservationSeatRepositoryImpl implements ReservationSeatRepositoryPort {

	private final JPAQueryFactory queryFactory;
	private final EntityManager entityManager;

	private final QReservationSeat qReservationSeat = QReservationSeat.reservationSeat;
	private final QReservationSeatDetail qReservationSeatDetail = QReservationSeatDetail.reservationSeatDetail;

	public ReservationSeatRepositoryImpl(JPAQueryFactory queryFactory, EntityManager entityManager) {
		this.queryFactory = queryFactory;
		this.entityManager = entityManager;
	}

	@Override
	public List<ReservationSeatDetail> getAvailableReservationSeatDetailList(long seatId) {
		List<ReservationSeatDetail> seatDetails = queryFactory.selectFrom(qReservationSeatDetail)
			.where(qReservationSeatDetail.seatId.eq(seatId)
				.and(qReservationSeatDetail.reservationStatus.eq(ReservationStatus.AVAILABLE)))
			.fetch();

		if (seatDetails.isEmpty()) {
			CustomException.nullPointer("해당 좌석을 찾을수 없습니다.", this.getClass());
		}

		return seatDetails;
	}

	@Override
	public List<ReservationSeat> getAvailableReservationSeatList(long concertScheduleId) {
		List<ReservationSeat> seats = queryFactory.selectFrom(qReservationSeat)
			.where(qReservationSeat.concertId.eq(concertScheduleId))
			.fetch();

		if (seats.isEmpty()) {
			CustomException.nullPointer("해당 콘서트 스케줄에 사용 가능한 좌석이 없습니다.", this.getClass());
		}

		return seats;
	}

	@Override
	public void updateReservationCurrentReserved(ReservationSeat reservationSeat) {
		queryFactory.update(qReservationSeat)
			.set(qReservationSeat.currentReserved, reservationSeat.getCurrentReserved())
			.where(qReservationSeat.seatId.eq(reservationSeat.getSeatId()))
			.execute();
	}

	@Override
	public ReservationSeatDetail getSeatDetailById(long seatDetailId) {
		ReservationSeatDetail seatDetail = queryFactory.selectFrom(qReservationSeatDetail)
			.where(qReservationSeatDetail.seatDetailId.eq(seatDetailId))
			.fetchOne();

		if (seatDetail == null) {
			CustomException.nullPointer("해당 좌석을 찾을수 없습니다.", this.getClass());
		}

		return seatDetail;
	}

	@Override
	public void updateSeatDetailStatus(ReservationSeatDetail seatDetail) {
		queryFactory.update(qReservationSeatDetail)
			.set(qReservationSeatDetail.reservationStatus, seatDetail.getReservationStatus())
			.where(qReservationSeatDetail.seatId.eq(seatDetail.getSeatDetailId()))
			.execute();
	}

	@Override
	public ReservationSeat getSeatById(long seatId) {
		ReservationSeat seat = queryFactory.selectFrom(qReservationSeat)
			.where(qReservationSeat.seatId.eq(seatId))
			.fetchOne();

		if (seat == null) {
			CustomException.nullPointer("좌석을 찾을 수 없습니다.", this.getClass());
		}

		return seat;
	}

    @Override
    public ReservationSeatDetail getSeatDetailByIdForUpdate(long seatDetailId) {
        ReservationSeatDetail seatDetail = queryFactory.selectFrom(qReservationSeatDetail)
            .where(qReservationSeatDetail.seatDetailId.eq(seatDetailId))
            .setLockMode(LockModeType.PESSIMISTIC_WRITE) // 비관적 잠금 설정
            .fetchOne();

        if (seatDetail == null) {
            CustomException.nullPointer("해당 좌석을 찾을 수 없습니다.", this.getClass());
        }

		// ReservationSeatDetail seatDetail = entityManager.find(ReservationSeatDetail.class, seatDetailId, LockModeType.PESSIMISTIC_WRITE);
		// if (seatDetail == null) {
		// 	 CustomException.nullPointer("해당 좌석을 찾을 수 없습니다.", this.getClass());
		// }

        return seatDetail;
    }
}
