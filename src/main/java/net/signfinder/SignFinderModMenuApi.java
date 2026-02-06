package net.signfinder;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import me.shedaniel.autoconfig.AutoConfigClient;

public final class SignFinderModMenuApi implements ModMenuApi
{
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory()
	{
        return parent -> AutoConfigClient
                .getConfigScreen(SignFinderConfig.class, parent).get();
	}
}
