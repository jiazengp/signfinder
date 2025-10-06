package net.signfinder.services;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.signfinder.SignFinderConfig;
import net.signfinder.SignFinderMod;
import net.signfinder.models.SignSearchResult;
import net.signfinder.util.SignTextUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles validation of cached sign data against current world state.
 * Determines if signs still exist and if their content has changed.
 */
public class DataValidationService
{
	private static final Logger LOGGER =
		LoggerFactory.getLogger(DataValidationService.class);
	
	/**
	 * Validates a cached sign result against the current world state.
	 *
	 * @param world
	 *            Current world instance
	 * @param pos
	 *            Position to validate
	 * @param cachedResult
	 *            Previously cached result
	 * @return Validation result indicating current status
	 */
	public ValidationResult validateSignAtPosition(World world, BlockPos pos,
		SignSearchResult cachedResult)
	{
		MinecraftClient client = MinecraftClient.getInstance();
		if(client.player == null)
		{
			return new ValidationResult(ValidationStatus.VALID, null);
		}
		
		SignFinderConfig config = SignFinderMod.getInstance().getConfig();
		double distance =
			Math.sqrt(pos.getSquaredDistance(client.player.getEntityPos()));
		boolean inRange = distance <= config.default_search_radius;
		
		try
		{
			// Check if chunk is loaded to avoid false negatives
			try
			{
				world.getBlockState(pos);
			}catch(Exception chunkException)
			{
				if(inRange)
				{
					LOGGER.debug(
						"Chunk not loaded at position {}, deferring validation",
						pos);
					return new ValidationResult(ValidationStatus.VALID, null);
				}else
				{
					LOGGER.debug(
						"Position {} out of range and chunk not loaded, assuming removed",
						pos);
					return new ValidationResult(ValidationStatus.REMOVED, null);
				}
			}
			
			// Check if sign still exists
			if(!isSignStillValid(world, pos))
			{
				LOGGER.debug("Sign no longer exists at position: {}", pos);
				return new ValidationResult(ValidationStatus.REMOVED, null);
			}
			
			// Get current text and compare
			String[] currentText = SignTextUtils.getSignText(world, pos);
			if(currentText == null)
			{
				LOGGER.debug("Could not read sign text at position: {}", pos);
				return new ValidationResult(ValidationStatus.REMOVED, null);
			}
			
			// Compare with cached text
			String[] cachedText = cachedResult.getSignText();
			if(java.util.Arrays.equals(currentText, cachedText))
			{
				return new ValidationResult(ValidationStatus.VALID, null);
			}else
			{
				// Text has changed, create updated result
				SignSearchResult updatedResult = new SignSearchResult(pos,
					client.player.getEntityPos(), currentText,
					String.join(" ", currentText), config.text_preview_length);
				LOGGER.debug("Sign text changed at position: {}", pos);
				return new ValidationResult(ValidationStatus.MODIFIED,
					updatedResult);
			}
		}catch(Exception e)
		{
			LOGGER.warn("Error during validation at position {}: {}", pos,
				e.getMessage());
			return new ValidationResult(ValidationStatus.VALID, null);
		}
	}
	
	/**
	 * Checks if a sign block still exists at the given position.
	 */
	public boolean isSignStillValid(World world, BlockPos pos)
	{
		try
		{
			return world.getBlockState(pos).getBlock().toString()
				.contains("sign");
		}catch(Exception e)
		{
			LOGGER.debug("Could not check block state at {}: {}", pos,
				e.getMessage());
			return false;
		}
	}
	
	/**
	 * Validates if an entity is within search range.
	 */
	public boolean isInSearchRange(Vec3d entityPos, Vec3d playerPos,
		double radius)
	{
		double distance = entityPos.distanceTo(playerPos);
		return distance <= radius;
	}
	
	public enum ValidationStatus
	{
		VALID, // Sign exists and content unchanged
		MODIFIED, // Sign exists but content has changed
		REMOVED // The Sign has been removed
	}
	
	public record ValidationResult(ValidationStatus status,
		SignSearchResult updatedResult)
	{}
}
