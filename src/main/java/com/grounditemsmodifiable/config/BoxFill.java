package com.grounditemsmodifiable.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum BoxFill
{
	FLAT("Flat"),
	GRADIENT("Gradient"),
	GLASS("Glass"),
	NONE("None");

	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
