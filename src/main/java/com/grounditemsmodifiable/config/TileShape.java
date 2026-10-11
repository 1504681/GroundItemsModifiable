package com.grounditemsmodifiable.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TileShape
{
	TILE("Whole tile"),
	CIRCLE("Circle");

	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
