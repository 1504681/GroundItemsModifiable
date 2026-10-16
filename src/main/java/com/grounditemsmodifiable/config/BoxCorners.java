package com.grounditemsmodifiable.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum BoxCorners
{
	SQUARE("Square"),
	ROUNDED("Rounded"),
	PILL("Pill"),
	NOTCHED("Notched");

	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
