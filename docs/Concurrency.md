```java
@Transactional(isolation = Isolation.READ_COMMITTED)
public TokenDto reserveSeat(long userId, long seatDetailId) {
	log.info("사용자 {}의 좌석 예약 시도가 시작되었습니다. 좌석 ID: {}", userId, seatDetailId);
	try {
		// Step 1: 대기열에 이미 등록된 사용자 확인
		if (waitingQueueService.isUserInQueue(userId, seatDetailId)) {
			throw new IllegalArgumentException("사용자가 이미 대기열에 등록되어 있습니다.");
		}

		// Step 2: 비관적 잠금을 사용하여 좌석 상세 정보 조회
		ReservationSeatDetailDto seatDetailDto = reservationService.getSeatDetailByIdForUpdate(seatDetailId);

		// Step 3: 좌석 상태 확인 및 예약 처리
		if (seatDetailDto.getReservationStatus() == ReservationStatus.AVAILABLE) {
			seatDetailDto.setReservationStatus(ReservationStatus.PENDING);
			reservationService.updateSeatDetailStatus(seatDetailDto);

			return tokenService.createToken(userId, 0, calculateRemainingTime(0), seatDetailId);
		} else {

			// 좌석이 AVAILABLE이 아닌 경우, 대기열에 사용자 추가
			WaitingQueueDto waitingId = waitingQueueService.addWaitingQueue(
				buildWaitingQueueDto(userId, seatDetailId));

			// 대기열 위치 계산
			int queuePosition = waitingQueueService.getQueuePosition(waitingId.getWaitingId());

			// 토큰 발급
			long remainingTime = calculateRemainingTime(queuePosition);
			return tokenService.createToken(userId, queuePosition, remainingTime, seatDetailId);
		}
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
```