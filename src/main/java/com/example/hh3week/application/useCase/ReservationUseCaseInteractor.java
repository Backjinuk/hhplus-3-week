package com.example.hh3week.application.useCase;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.example.hh3week.adapter.in.dto.reservation.ReservationSeatDetailDto;
import com.example.hh3week.adapter.in.dto.reservation.ReservationSeatDto;
import com.example.hh3week.adapter.in.dto.token.TokenDto;
import com.example.hh3week.adapter.in.dto.waitingQueue.WaitingQueueDto;
import com.example.hh3week.application.port.in.ReservationUseCase;
import com.example.hh3week.application.service.ReservationService;
import com.example.hh3week.application.service.TokenService;
import com.example.hh3week.application.service.WaitingQueueService;
import com.example.hh3week.domain.reservation.entity.ReservationSeatDetail;
import com.example.hh3week.domain.reservation.entity.ReservationStatus;
import com.example.hh3week.domain.waitingQueue.entity.WaitingQueue;
import com.example.hh3week.domain.waitingQueue.entity.WaitingStatus;

import jakarta.persistence.OptimisticLockException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ReservationUseCaseInteractor implements ReservationUseCase {

	private final ReservationService reservationService;
	private final WaitingQueueService waitingQueueService;
	private final TokenService tokenService;
	private final AtomicLong tokenIdGenerator = new AtomicLong(1);

	public ReservationUseCaseInteractor(ReservationService reservationService, WaitingQueueService waitingQueueService,
		TokenService tokenService) {
		this.reservationService = reservationService;
		this.waitingQueueService = waitingQueueService;
		this.tokenService = tokenService;
	}

	@Override
	public List<ReservationSeatDto> getAvailableReservationSeatList(long concertScheduleId) {
		return reservationService.getAvailableReservationSeatList(concertScheduleId)
			.stream()
			.peek(reservationSeatDto -> reservationSeatDto.setReservationSeatDetailDtoList(
				reservationService.getAvailableReservationSeatDetailList(reservationSeatDto.getSeatId())))
			.toList();
	}

	@Retryable(
		value = { OptimisticLockingFailureException.class },
		maxAttempts = 100,
		backoff = @Backoff(delay = 50, multiplier = 1.2)
	)
	@Transactional
	public TokenDto reserveSeat(long userId, long seatDetailId) {
		log.info("사용자 {}의 좌석 예약 시도가 시작되었습니다. 좌석 ID: {}", userId, seatDetailId);
		try {
			// Step 1: 대기열에 이미 등록된 사용자 확인
			if (waitingQueueService.isUserInQueue(userId, seatDetailId)) {
				throw new IllegalArgumentException("사용자가 이미 대기열에 등록되어 있습니다.");
			}

			// Step 2: 낙관적 락을 사용하여 좌석 상세 정보 조회
			ReservationSeatDetail seatDetail = reservationService.getSeatDetailById2(seatDetailId);

			// Step 3: 좌석 상태 확인 및 예약 처리
			if (seatDetail.getReservationStatus() == ReservationStatus.AVAILABLE) {
				seatDetail.setReservationStatus(ReservationStatus.PENDING);
				reservationService.updateSeatDetailStatus2(seatDetail); // @Version 필드 자동 업데이트

				log.info("사용자 {}의 좌석 예약 성공. 좌석 ID: {}", userId, seatDetailId);
				return tokenService.createToken(userId, 0, calculateRemainingTime(0), seatDetailId);
			} else {
				// 좌석이 AVAILABLE이 아닌 경우, 대기열에 사용자 추가
				WaitingQueue waitingId = waitingQueueService.addWaitingQueue2(buildWaitingQueue(userId, seatDetailId));

				// 대기열 위치 계산
				int queuePosition = waitingQueueService.getQueuePosition(waitingId.getWaitingId());

				// 토큰 발급
				long remainingTime = calculateRemainingTime(queuePosition);
				log.info("사용자 {}의 대기열 등록 완료. 좌석 ID: {}, 대기열 위치: {}", userId, seatDetailId, queuePosition);
				return tokenService.createToken(userId, queuePosition, remainingTime, seatDetailId);
			}
		} catch (OptimisticLockingFailureException e) {
			log.error("사용자 {}의 좌석 예약 중 낙관적 락 충돌 발생: {}", userId, e.getMessage());
			throw e; // 재시도를 위해 예외를 다시 던짐
		} catch (CannotAcquireLockException | DeadlockLoserDataAccessException e) {
			log.error("사용자 {}의 좌석 예약 중 데드락 또는 락 획득 실패 발생: {}", userId, e.getMessage());
			throw e; // 재시도를 위해 예외를 다시 던짐
		} catch (Exception e) {
			log.error("사용자 {}의 좌석 예약 중 예상치 못한 오류 발생: {}", userId, e.getMessage(), e);
			throw e;
		} finally {
			log.info("사용자 {}의 좌석 예약 시도 종료. 좌석 ID: {}", userId, seatDetailId);
		}
	}

	@Recover
	public TokenDto recover(OptimisticLockingFailureException e, long userId, long seatDetailId) {
		log.warn("사용자 {}의 좌석 예약 재시도 실패. 좌석 ID: {}", userId, seatDetailId);
		throw new IllegalArgumentException("좌석 예약에 실패했습니다.", e);
	}

	/**
	 * 대기열에서 남은 시간을 계산하는 메서드 (예시)
	 *
	 * @param queuePosition 대기열에서의 위치
	 * @return 남은 대기 시간 (초 단위)
	 */
	private long calculateRemainingTime(int queuePosition) {
		// 예시: 각 사용자당 10분의 대기 시간 부여
		return queuePosition * 300L;
	}

	private WaitingQueueDto buildWaitingQueueDto(long userId, long seatDetailId) {
		return WaitingQueueDto.builder()
			.userId(userId)
			.seatDetailId(seatDetailId)
			.waitingStatus(WaitingStatus.WAITING)
			.reservationDt(LocalDateTime.now())
			.build();
	}

	private WaitingQueue buildWaitingQueue(long userId, long seatDetailId) {
		return WaitingQueue.builder()
			.userId(userId)
			.seatDetailId(seatDetailId)
			.waitingStatus(WaitingStatus.WAITING)
			.reservationDt(LocalDateTime.now())
			.build();
	}
}