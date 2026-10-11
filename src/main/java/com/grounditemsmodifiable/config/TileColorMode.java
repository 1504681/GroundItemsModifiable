package com.grounditemsmodifiable.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TileColorMode
{
	ITEM_COLOR("Item colour"),
	CUSTOM("Custom colour");

	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
