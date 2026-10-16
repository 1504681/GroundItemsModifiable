package com.grounditemsmodifiable.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Palette
{
	NORMAL("Normal"),
	PASTEL("Pastel");

	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
