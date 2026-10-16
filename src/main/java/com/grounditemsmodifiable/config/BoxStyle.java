package com.grounditemsmodifiable.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum BoxStyle
{
	BOX("Full box"),
	STRIPE("Accent stripe");

	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
