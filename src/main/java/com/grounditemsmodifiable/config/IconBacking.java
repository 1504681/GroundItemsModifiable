package com.grounditemsmodifiable.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum IconBacking
{
	NONE("None"),
	SQUARE("Square"),
	CIRCLE("Circle");

	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
