package com.example.hh3week.adapter.out.streaming.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserQueueValidationRequest {
	private long userId;
	private long seatDetailId;


}