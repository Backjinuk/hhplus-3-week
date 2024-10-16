// src/test/java/com/example/hh3week/adapter/in/controller/ReservationControllerTest.java

package com.example.hh3week.adapter.in.controller;

import com.example.hh3week.adapter.in.dto.reservation.ReservationSeatDto;
import com.example.hh3week.adapter.in.dto.token.TokenDto;
import com.example.hh3week.application.port.in.ReservationUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReservationController.class)
public class ReservationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ReservationUseCase reservationUseCase;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	@DisplayName("예약 가능한 좌석 목록 조회 성공")
	void getAvailableReservationSeatList_Success() throws Exception {
		// Given
		long concertScheduleId = 1L;
		Map<String, Long> requestBody = new HashMap<>();
		requestBody.put("concertScheduleId", concertScheduleId);

		ReservationSeatDto seat1 = ReservationSeatDto.builder()
			.seatId(101L)
			.concertId(1L)
			.maxCapacity(100L)
			.currentReserved(50L)
			.build();

		ReservationSeatDto seat2 = ReservationSeatDto.builder()
			.seatId(102L)
			.concertId(1L)
			.maxCapacity(100L)
			.currentReserved(60L)
			.build();

		List<ReservationSeatDto> expectedSeats = Arrays.asList(seat1, seat2);

		when(reservationUseCase.getAvailableReservationSeatList(concertScheduleId))
			.thenReturn(expectedSeats);

		// When & Then
		mockMvc.perform(post("/api/v1/reservations/getAvailableReservationSeatList")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestBody)))
			.andExpect(status().isOk())
			.andExpect(content().json(objectMapper.writeValueAsString(expectedSeats)));

		verify(reservationUseCase, times(1)).getAvailableReservationSeatList(concertScheduleId);
	}

	@Test
	@DisplayName("좌석 예약 요청 성공")
	void reserveSeat_Success() throws Exception {
		// Given
		long userId = 10L;
		long seatDetailId = 1001L;
		Map<String, Long> requestBody = new HashMap<>();
		requestBody.put("userId", userId);
		requestBody.put("seatDetailId", seatDetailId);

		TokenDto tokenDto = TokenDto.builder()
			.token("sample-token-12345")
			.expiresAt(LocalDateTime.now())
			.build();

		when(reservationUseCase.reserveSeat(userId, seatDetailId))
			.thenReturn(tokenDto);

		// When & Then
		mockMvc.perform(post("/api/v1/reservations/reserveSeat")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestBody)))
			.andExpect(status().isOk())
			.andExpect(content().json(objectMapper.writeValueAsString(tokenDto)));

		verify(reservationUseCase, times(1)).reserveSeat(userId, seatDetailId);
	}

	@Test
	@DisplayName("예약 가능한 좌석 조회 시 concertScheduleId 누락")
	void getAvailableReservationSeatList_MissingConcertScheduleId() throws Exception {
		// Given
		Map<String, Long> requestBody = new HashMap<>();
		// concertScheduleId 누락

		// When & Then
		mockMvc.perform(post("/api/v1/reservations/getAvailableReservationSeatList")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestBody)))
			.andExpect(status().isBadRequest())
			.andExpect(content().string("concertScheduleId는 필수 입력 항목입니다."));
	}

	@Test
	@DisplayName("좌석 예약 요청 시 userId 누락")
	void reserveSeat_MissingUserId() throws Exception {
		// Given
		long seatDetailId = 1001L;
		Map<String, Long> requestBody = new HashMap<>();
		// userId 누락
		requestBody.put("seatDetailId", seatDetailId);

		// When & Then
		mockMvc.perform(post("/api/v1/reservations/reserveSeat")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestBody)))
			.andExpect(status().isBadRequest())
			.andExpect(content().string("userId와 seatDetailId는 필수 입력 항목입니다."));
	}

	@Test
	@DisplayName("좌석 예약 요청 시 seatDetailId 누락")
	void reserveSeat_MissingSeatDetailId() throws Exception {
		// Given
		long userId = 10L;
		Map<String, Long> requestBody = new HashMap<>();
		requestBody.put("userId", userId);
		// seatDetailId 누락

		// When & Then
		mockMvc.perform(post("/api/v1/reservations/reserveSeat")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestBody)))
			.andExpect(status().isBadRequest())
			.andExpect(content().string("userId와 seatDetailId는 필수 입력 항목입니다."));
	}
}
