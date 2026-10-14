package com.grounditemsmodifiable.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TimerVisibility
{
	ALWAYS("Always"),
	PUBLIC_OR_PULSING("Public or pulsing"),
	UNDER_TICKS("Under N ticks left");

	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
