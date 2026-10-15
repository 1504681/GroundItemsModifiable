package com.grounditemsmodifiable.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum QuantityFormat
{
	TIMES("x 3"),
	PARENTHESES("(3)"),
	HIDDEN("Hidden");

	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
