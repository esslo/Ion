package net.horizonsend.ion.server.features.multiblock.type.defense.passive.areashield

import net.horizonsend.ion.server.features.multiblock.shape.MultiblockShape

object AreaShield20 : AreaShield(radius = 20) {
	override fun MultiblockShape.buildStructure() {
		z(+0) {
			y(-1) {
				x(-2).anyStairs()
				x(-1).anyStairs()
				x(+0).powerInput()
				x(+1).anyStairs()
				x(+2).anyStairs()
			}

			y(+0) {
				x(-1).anyGlassPane()
				x(+0).anyGlass()
				x(+1).anyGlassPane()
			}

			y(+1) {
				x(-1).anyGlassPane()
				x(+0).anyGlass()
				x(+1).anyGlassPane()
			}
		}

		z(+1) {
			y(-1) {
				x(-2).ironBlock()
				x(-1).ironBlock()
				x(+0).sponge()
				x(+1).ironBlock()
				x(+2).ironBlock()
			}

			y(+0) {
				x(-1).anyGlass()
				x(+0).sponge()
				x(+1).anyGlass()
			}

			y(+1) {
				x(-1).anyGlass()
				x(+0).sponge()
				x(+1).anyGlass()
			}
		}

		z(+2) {
			y(-1) {
				x(-2).anyStairs()
				x(-1).solidBlock()
				x(+0).diamondBlock()
				x(+1).solidBlock()
				x(+2).anyStairs()
			}

			y(+0) {
				x(-1).anyGlassPane()
				x(+0).anyGlass()
				x(+1).anyGlassPane()
			}

			y(+1) {
				x(-1).anyGlassPane()
				x(+0).anyGlass()
				x(+1).anyGlassPane()
			}
		}
	}
}
