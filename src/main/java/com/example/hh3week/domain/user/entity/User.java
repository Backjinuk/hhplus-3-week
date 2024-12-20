package com.example.hh3week.domain.user.entity;

import com.example.hh3week.adapter.in.dto.user.UserDto;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long userId;

	private String userName;

	private long pointBalance;

	// @Version
	// private long version;

	@Builder
	public User(long userId, String userName, long pointBalance) {
		this.userId = userId;
		this.userName = userName;
		this.pointBalance = pointBalance;
	}

	public static User toEntity(UserDto userDto){
		return User.builder()
			.userId(userDto.getUserId())
			.userName(userDto.getUserName())
			.pointBalance(userDto.getPointBalance())
			.build();
	}
}
