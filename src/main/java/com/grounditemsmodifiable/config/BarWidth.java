package com.grounditemsmodifiable.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum BarWidth
{
	FULL("Icon and name"),
	ICON("Icon only");

	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
