package net.signfinder;

import net.fabricmc.api.ModInitializer;

public final class SignFinderModInitializer implements ModInitializer
{
	private static SignFinderMod instance;
	
	@Override
	public void onInitialize()
	{
		if(instance != null)
			throw new RuntimeException(
				"SignFinderMod.onInitialize() ran twice!");
		instance = new SignFinderMod();
	}
	
	public static SignFinderMod getInstance()
	{
		return instance;
	}
}
