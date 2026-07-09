package net.signfinder.mixin;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Selectively applies mixins based on the Minecraft version.
 * <ul>
 * <li>26.1.x — applies {@link WorldRendererMixin} (targets
 * {@code renderLevel})</li>
 * <li>26.2.x — applies {@link WorldRendererMixin26} (targets
 * {@code render})</li>
 * </ul>
 */
public class SignFinderMixinPlugin implements IMixinConfigPlugin
{
	@Override
	public boolean shouldApplyMixin(String targetClassName,
		String mixinClassName)
	{
		if(!mixinClassName
			.startsWith("net.signfinder.mixin.WorldRendererMixin"))
			return true;
		
		try
		{
			Version mc = FabricLoader.getInstance().getModContainer("minecraft")
				.orElseThrow().getMetadata().getVersion();
			
			boolean is26_2orLater = mc.compareTo(Version.parse("26.2.0")) >= 0;
			
			if(mixinClassName.endsWith("26"))
				return is26_2orLater; // WorldRendererMixin26 only on 26.2+
			else
				return !is26_2orLater; // WorldRendererMixin only on 26.1.x
		}catch(VersionParsingException e)
		{
			// If we can't determine the version, don't apply either
			// version-specific mixin — applying both would crash.
			return false;
		}
	}
	
	@Override
	public void onLoad(String mixinPackage)
	{}
	
	@Override
	public String getRefMapperConfig()
	{
		return null;
	}
	
	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets)
	{}
	
	@Override
	public List<String> getMixins()
	{
		return null;
	}
	
	@Override
	public void preApply(String targetClassName, ClassNode targetClass,
		String mixinClassName, IMixinInfo mixinInfo)
	{}
	
	@Override
	public void postApply(String targetClassName, ClassNode targetClass,
		String mixinClassName, IMixinInfo mixinInfo)
	{}
}
