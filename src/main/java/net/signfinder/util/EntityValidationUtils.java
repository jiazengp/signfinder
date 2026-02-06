package net.signfinder.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import net.signfinder.SignFinderConfig;
import net.signfinder.SignFinderMod;
import net.signfinder.models.SignSearchResult;

/**
 * Utility class for common entity validation operations.
 * Consolidates repeated validation logic across the codebase.
 */
public enum EntityValidationUtils
{
	;
	
	private static final Logger LOGGER =
		LoggerFactory.getLogger(EntityValidationUtils.class);
	
	/**
	 * Validates a sign entity against cached data.
	 * Checks if the sign still exists and if its content has changed.
	 */
	public static ValidationResult validateSignEntity(Level world, BlockPos pos,
                                                      SignSearchResult cached)
	{
		Minecraft client = Minecraft.getInstance();
		if(client.player == null)
		{
			return new ValidationResult(ValidationStatus.VALID, null);
		}
		
		SignFinderConfig config = SignFinderMod.getInstance().getConfig();
		double distance =
			Math.sqrt(pos.distToCenterSqr(client.player.position()));
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
			String[] cachedText = cached.getSignText();
			if(java.util.Arrays.equals(currentText, cachedText))
			{
				return new ValidationResult(ValidationStatus.VALID, null);
			}else
			{
				// Text has changed, create updated result
				SignSearchResult updatedResult = new SignSearchResult(pos,
					client.player.position(), currentText,
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
	public static boolean isSignStillValid(Level world, BlockPos pos)
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
	public static boolean isInSearchRange(Vec3 entityPos, Vec3 playerPos,
                                          double radius)
	{
		double distance = entityPos.distanceTo(playerPos);
		return distance <= radius;
	}
	
	/**
	 * Validates if a position is within the specified radius from a center
	 * point.
	 */
	public static boolean isWithinRadius(BlockPos pos, Vec3 center,
		double radius)
	{
		Vec3 posVec = pos.getCenter();
		return posVec.distanceTo(center) <= radius;
	}
	
	/**
	 * Common validation status enum.
	 */
	public enum ValidationStatus
	{
		VALID, // Entity exists and content unchanged
		MODIFIED, // Entity exists but content has changed
		REMOVED // Entity has been removed
	}
	
	/**
	 * Result class for validation operations.
	 */
	public static class ValidationResult
	{
		public final ValidationStatus status;
		public final SignSearchResult updatedResult;
		
		public ValidationResult(ValidationStatus status,
			SignSearchResult updatedResult)
		{
			this.status = status;
			this.updatedResult = updatedResult;
		}
	}
}
