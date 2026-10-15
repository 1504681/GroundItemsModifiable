package com.grounditemsmodifiable.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum CollapsedQuantity
{
	NUMBER("Number only (1,000)"),
	SAME_AS_NAMES("Same as names"),
	OFF("Off");

	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
