package com.grounditemsmodifiable.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum BoxFillColor
{
	TIER("Tier colour"),
	DARK("Dark"),
	STONE("Stone");

	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
