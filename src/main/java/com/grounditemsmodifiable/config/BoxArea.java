package com.grounditemsmodifiable.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum BoxArea
{
	ICON("Icon only"),
	ICON_AND_NAME("Icon and name"),
	NAME("Name only");

	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
