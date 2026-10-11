package com.grounditemsmodifiable.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TileFillMode
{
	ITEM_COLOR("Item colour"),
	CUSTOM("Custom colour"),
	NONE("No fill");

	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
