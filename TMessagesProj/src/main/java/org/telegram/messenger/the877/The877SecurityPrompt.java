package org.telegram.messenger.the877;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Simple UI bridge for setting and checking the separate The 877 hidden-chats PIN.
 *
 * This helper does not open the hidden chats screen. The caller must do that only
 * after the onAuthenticated callback is invoked.
 */
public final class The877SecurityPrompt {

    public interface Callback {
        void onAuthenticated();
    }

    private The877SecurityPrompt() {
    }

    /** Shows first-time PIN setup, or replacement after the caller has authenticated. */
    public static void showSetPin(final Activity activity, final int account,
                                  final Runnable onSaved) {
        if (!isActivityUsable(activity)) {
            return;
        }

        final EditText firstPin = createPinInput(activity);
        showInputDialog(activity,
                "إنشاء الرمز السري",
                "أدخل رمزاً من 6 إلى 12 رقماً",
                firstPin,
                "متابعة",
                () -> {
                    String pin = firstPin.getText().toString();
                    if (!isValidPin(pin)) {
                        toast(activity, "الرمز لازم يكون من 6 إلى 12 رقم");
                        return false;
                    }
                    confirmPin(activity, account, pin, onSaved);
                    return true;
                });
    }

    private static void confirmPin(final Activity activity, final int account,
                                   final String firstPin, final Runnable onSaved) {
        if (!isActivityUsable(activity)) {
            return;
        }

        final EditText confirmPin = createPinInput(activity);
        showInputDialog(activity,
                "تأكيد الرمز السري",
                "أعد إدخال الرمز للتأكيد",
                confirmPin,
                "حفظ الرمز",
                () -> {
                    String secondPin = confirmPin.getText().toString();
                    if (!firstPin.equals(secondPin)) {
                        toast(activity, "الرمزان غير متطابقين، حاول مرة ثانية");
                        return false;
                    }
                    if (!The877SecurityManager.setPin(account, firstPin)) {
                        toast(activity, "تعذر حفظ الرمز. حاول مرة ثانية");
                        return false;
                    }
                    toast(activity, "تم حفظ الرمز السري");
                    if (onSaved != null) {
                        onSaved.run();
                    }
                    return true;
                });
    }

    /** Requests the existing hidden-chats PIN and invokes the callback only on success. */
    public static void showUnlock(final Activity activity, final int account,
                                  final Callback callback) {
        if (!isActivityUsable(activity)) {
            return;
        }

        if (!The877SecurityManager.hasPin(account)) {
            toast(activity, "عيّن رمزاً سرياً أولاً");
            return;
        }

        long remaining = The877SecurityManager.getLockRemainingMillis(account);
        if (remaining > 0L) {
            long seconds = Math.max(1L, (remaining + 999L) / 1000L);
            toast(activity, "تم إيقاف المحاولة مؤقتاً. حاول بعد " + seconds + " ثانية");
            return;
        }

        final EditText pinInput = createPinInput(activity);
        showInputDialog(activity,
                "المحادثات المخفية",
                "أدخل الرمز السري للمتابعة",
                pinInput,
                "فتح",
                () -> {
                    String pin = pinInput.getText().toString();
                    if (The877SecurityManager.verifyPin(account, pin)) {
                        if (callback != null) {
                            callback.onAuthenticated();
                        }
                        return true;
                    }
                    long wait = The877SecurityManager.getLockRemainingMillis(account);
                    if (wait > 0L) {
                        long seconds = Math.max(1L, (wait + 999L) / 1000L);
                        toast(activity, "محاولات كثيرة. انتظر " + seconds + " ثانية");
                    } else {
                        toast(activity, "الرمز السري غير صحيح");
                    }
                    return false;
                });
    }

    private interface ValidationAction {
        boolean run();
    }

    private static void showInputDialog(final Activity activity, String title, String message,
                                        EditText input, String positive,
                                        ValidationAction onPositive) {
        LinearLayout container = new LinearLayout(activity);
        container.setOrientation(LinearLayout.VERTICAL);
        int horizontal = dp(activity, 24);
        container.setPadding(horizontal, dp(activity, 8), horizontal, dp(activity, 4));

        TextView hint = new TextView(activity);
        hint.setText(message);
        hint.setTextSize(14);
        container.addView(hint, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 52));
        inputParams.topMargin = dp(activity, 12);
        container.addView(input, inputParams);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(title)
                .setView(container)
                .setPositiveButton(positive, null)
                .setNegativeButton("إلغاء", (d, which) -> d.dismiss())
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    if (onPositive == null || onPositive.run()) {
                        dialog.dismiss();
                    }
                }));

        dialog.show();
    }

    private static EditText createPinInput(Context context) {
        EditText input = new EditText(context);
        input.setSingleLine(true);
        input.setTextSize(18);
        input.setGravity(android.view.Gravity.CENTER);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setTransformationMethod(PasswordTransformationMethod.getInstance());
        input.setHint("••••••");
        input.setSelectAllOnFocus(true);
        return input;
    }

    private static boolean isValidPin(String pin) {
        if (pin == null || pin.length() < 6 || pin.length() > 12) {
            return false;
        }
        for (int i = 0; i < pin.length(); i++) {
            if (pin.charAt(i) < '0' || pin.charAt(i) > '9') {
                return false;
            }
        }
        return true;
    }

    private static boolean isActivityUsable(Activity activity) {
        return activity != null && !activity.isFinishing()
                && (android.os.Build.VERSION.SDK_INT < 17 || !activity.isDestroyed());
    }

    private static int dp(Context context, float value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    private static void toast(Activity activity, String message) {
        if (isActivityUsable(activity)) {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
        }
    }
}
