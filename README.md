# MCOverlayExtractor32
Working on a texture extraction algorithm to generate ore overlays for Ore Stone Variants, a Minecraft mod. Unlike the original, it currently features shading and (in rare cases) fancy Conquest textures. It also just doesn't always work.

	/**
	 * This program is a rewrite of the original texture extraction algorithm written by
	 * pupnewfster, modified by myself. It is considerably more complicated than the
	 * original, which may or may not be for the best. My purpose in rewriting this algorithm
	 * is to expand compatibility to more ores with the following goals:
	 * 
	 *     * Better support for the 32x textures made by the Conquest team, which are far more
	 *       complex than most textures from vanilla and other modded textures made in the
	 *       same style; and
	 *     * Support for all or most textures which were previously supported to begin with.
	 *       More specifically, to do so without having to iterate on a loop based on some
	 *       nearly arbitrary percentage of alpha pixels which is not applicable for all
	 *       ores.
	 *       
	 *     In the original algorithm, a threshold was used for determining which amount of
	 * difference between each pixel, background to foreground, indicated that the pixel
	 * was a part of the texture's ore sprite. Using a single threshold proved to be
	 * somewhat problematic; higher thresholds were better for avoiding too many pixels,
	 * whereas lower thresholds were really necessary for textures like those from Base Metals,
	 * which tend to be a little less colorful and thereby don't stand out as much.
	 * 
	 *     A change was made to allow the program to retry if it thought it didn't find enough
	 * pixels. In short, early versions of this revision capitalized on this solution, by
	 * working to estimate the percentage of pixels that should be included, we can gradually
	 * modify that threshold to achieve the most appropriate alpha percentage. This solution is
	 * too complicated and ultimately doesn't make sense since it works by interpreting which
	 * pixels are likely to be ore pixels to begin with. Some of that methodology has made its
	 * way into algorithm2(), getComparisonColors(), and IMGTools#getOrePixel2() instead.
	 * 
	 *     In its current state, some of the 32x textures which were originally working well
	 * no longer produce similar results. Moreover, a number of 16x vanilla-style textures 
	 * are also showing issues. To combat this, the program alternates between two different
	 * algorithms based on the degree of variance between the pixels of the original texture.
	 * However, there are still issues with each solution. The program will output both the
	 * greatest difference and likewise which algorithm was chosen, which can help in
	 * diagnosing where the problem lies on a per-output basis.
	 * 
	 *     Unfortunately, despite these compromises, it still seems ideal to move away from
	 * the original threshold-based algorithm in pursuit of generating overlays from more
	 * complex images. Tweaks to some of the values below and in IMGTools.class can restore
	 * the functionality described above, albeit inconsistently. The solution in moving
	 * forward is to better understand how to modify these values as needed, or to devise a
	 * more clever algorithm than the two currently used. 
	 */
