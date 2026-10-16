package com.grounditemsmodifiable.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum BoxBorder
{
	SOLID("Solid"),
	DOUBLE("Double"),
	DARK_EDGE("Solid, dark edge"),
	NONE("None");

	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
