package net.signfinder.util;

/**
 * 颜色处理工具类，用于处理ARGB颜色格式转换和透明度计算。
 */
public enum ColorUtils
{
	;
	
	/**
	 * 将RGB颜色和透明度值组合成ARGB颜色。
	 *
	 * @param rgb
	 *            RGB颜色值（例如：0x00FF00）
	 * @param alpha
	 *            透明度值（0-255，0=完全透明，255=完全不透明）
	 * @return ARGB颜色值
	 */
	public static int combineRgbWithAlpha(int rgb, int alpha)
	{
		// 确保alpha在有效范围内
		alpha = Math.max(0, Math.min(255, alpha));
		
		// 清除RGB值中可能存在的alpha通道
		rgb = rgb & 0x00FFFFFF;
		
		// 将alpha移到最高字节并与RGB组合
		return (alpha << 24) | rgb;
	}
	
	/**
	 * 从ARGB颜色值中提取RGB部分。
	 *
	 * @param argb
	 *            ARGB颜色值
	 * @return RGB颜色值（不包含alpha通道）
	 */
	public static int extractRgb(int argb)
	{
		return argb & 0x00FFFFFF;
	}
	
	/**
	 * 从ARGB颜色值中提取alpha通道。
	 *
	 * @param argb
	 *            ARGB颜色值
	 * @return alpha值（0-255）
	 */
	public static int extractAlpha(int argb)
	{
		return (argb >> 24) & 0xFF;
	}
	
	/**
	 * 从ARGB颜色值中提取红色分量。
	 *
	 * @param argb
	 *            ARGB颜色值
	 * @return 红色值（0-255）
	 */
	public static int extractRed(int argb)
	{
		return (argb >> 16) & 0xFF;
	}
	
	/**
	 * 从ARGB颜色值中提取绿色分量。
	 *
	 * @param argb
	 *            ARGB颜色值
	 * @return 绿色值（0-255）
	 */
	public static int extractGreen(int argb)
	{
		return (argb >> 8) & 0xFF;
	}
	
	/**
	 * 从ARGB颜色值中提取蓝色分量。
	 *
	 * @param argb
	 *            ARGB颜色值
	 * @return 蓝色值（0-255）
	 */
	public static int extractBlue(int argb)
	{
		return argb & 0xFF;
	}
	
	/**
	 * 创建ARGB颜色值。
	 *
	 * @param alpha
	 *            透明度（0-255）
	 * @param red
	 *            红色分量（0-255）
	 * @param green
	 *            绿色分量（0-255）
	 * @param blue
	 *            蓝色分量（0-255）
	 * @return ARGB颜色值
	 */
	public static int createArgb(int alpha, int red, int green, int blue)
	{
		return (alpha << 24) | (red << 16) | (green << 8) | blue;
	}
	
	/**
	 * 将透明度百分比转换为alpha值。
	 *
	 * @param transparencyPercent
	 *            透明度百分比（0-100，0=完全不透明，100=完全透明）
	 * @return alpha值（0-255）
	 */
	public static int transparencyPercentToAlpha(int transparencyPercent)
	{
		transparencyPercent = Math.max(0, Math.min(100, transparencyPercent));
		return 255 - (transparencyPercent * 255 / 100);
	}
	
	/**
	 * 将alpha值转换为透明度百分比。
	 *
	 * @param alpha
	 *            alpha值（0-255）
	 * @return 透明度百分比（0-100）
	 */
	public static int alphaToTransparencyPercent(int alpha)
	{
		alpha = Math.max(0, Math.min(255, alpha));
		return 100 - (alpha * 100 / 255);
	}
	
	/**
	 * 将颜色名称转换为ARGB格式的字符串表示。
	 *
	 * @param argb
	 *            ARGB颜色值
	 * @return 格式化的颜色字符串
	 */
	public static String toArgbString(int argb)
	{
		int alpha = extractAlpha(argb);
		int red = extractRed(argb);
		int green = extractGreen(argb);
		int blue = extractBlue(argb);
		
		return String.format("ARGB(%d, %d, %d, %d)", alpha, red, green, blue);
	}
	
	/**
	 * 将颜色值转换为十六进制字符串。
	 *
	 * @param argb
	 *            ARGB颜色值
	 * @return 十六进制颜色字符串（例如：#80FF0000）
	 */
	public static String toHexString(int argb)
	{
		return String.format("#%08X", argb);
	}
}
