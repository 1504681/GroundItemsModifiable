package com.grounditemsmodifiable.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TextStyle
{
	NORMAL("Normal"),
	BOLD("Bold");

	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
