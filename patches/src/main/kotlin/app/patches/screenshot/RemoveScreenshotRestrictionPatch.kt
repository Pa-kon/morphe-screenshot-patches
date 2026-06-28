package app.patches.screenshot

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

private const val EXTENSION_CLASS =
    "Lapp/patches/extension/screenshot/ScreenshotRestrictionPatch;"

private val INSTAGRAM = Compatibility(
    name = "Instagram",
    packageName = "com.instagram.android",
    apkFileType = ApkFileType.APKM,
    appIconColor = 0xFC483C,
    targets = listOf(
        AppTarget(version = "435.0.0.37.76"),
        // Add newer versions here as they come out — the patch is version-agnostic
        // because it targets a stable Android framework API, not obfuscated IG code.
        AppTarget(version = null), // null = accept any future version
    )
)

@Suppress("unused")
val removeScreenshotRestrictionPatch = bytecodePatch(
    name = "Remove screenshot restriction",
    description = "Removes FLAG_SECURE from all windows so screenshots and screen recordings work in Instagram.",
    default = true,
) {
    compatibleWith(INSTAGRAM)

    extendWith("extensions/screenshot.mpe")

    execute {
        /*
         * Strategy: scan every method in the app.
         * Whenever we find an invoke-virtual targeting:
         *   android/view/Window;->addFlags(I)V
         *   android/view/Window;->setFlags(II)V
         * we replace it with an invoke-static to our extension class.
         *
         * The register layout is identical between virtual and static calls:
         *   virtual {v_window, v_flags}        → static {v_window, v_flags}
         *   virtual {v_window, v_flags, v_mask} → static {v_window, v_flags, v_mask}
         * because invoke-virtual already puts the object ref as the first operand.
         */
        classes.forEach { classDef ->
            classDef.methods
                .filter { it.implementation != null }
                .forEach { method ->
                    val toReplace = mutableListOf<Pair<Int, String>>()

                    method.implementation!!.instructions.forEachIndexed { index, instruction ->
                        if (instruction.opcode != Opcode.INVOKE_VIRTUAL) return@forEachIndexed

                        val ref = (instruction as? ReferenceInstruction)
                            ?.reference?.toString()
                            ?: return@forEachIndexed

                        val regs = instruction as FiveRegisterInstruction

                        when (ref) {
                            "Landroid/view/Window;->addFlags(I)V" -> {
                                val vWindow = regs.registerC
                                val vFlags  = regs.registerD
                                toReplace += index to
                                    "invoke-static {v$vWindow, v$vFlags}, " +
                                    "${EXTENSION_CLASS}->addFlags(Landroid/view/Window;I)V"
                            }
                            "Landroid/view/Window;->setFlags(II)V" -> {
                                val vWindow = regs.registerC
                                val vFlags  = regs.registerD
                                val vMask   = regs.registerE
                                toReplace += index to
                                    "invoke-static {v$vWindow, v$vFlags, v$vMask}, " +
                                    "${EXTENSION_CLASS}->setFlags(Landroid/view/Window;II)V"
                            }
                        }
                    }

                    // Replace in reverse order so earlier indices stay valid.
                    toReplace.asReversed().forEach { (idx, smali) ->
                        method.replaceInstruction(idx, smali)
                    }
                }
        }
    }
}
