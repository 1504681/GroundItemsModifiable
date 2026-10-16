package com.grounditemsmodifiable.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum BoxTheme
{
	CLASSIC("Classic"),
	OSRS("OSRS"),
	MINIMAL("Minimal"),
	NEON("Neon"),
	PASTEL("Pastel"),
	CUSTOM("Custom");

	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
