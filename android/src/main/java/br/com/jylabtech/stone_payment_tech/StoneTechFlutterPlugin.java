package br.com.jylabtech.stone_payment_tech;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.NonNull;
import br.com.jylabtech.stone_payment_tech.core.StoneTech;
import br.com.jylabtech.stone_payment_tech.payments.PaymentsFragment;
import br.com.jylabtech.stone_payment_tech.payments.PaymentsPresenter;
import br.com.jylabtech.stone_payment_tech.payments.PaymentsUseCase;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

public class StoneTechFlutterPlugin
  implements FlutterPlugin, MethodCallHandler {

  private static final String CHANNEL_NAME = "stone_payment_tech";
  private MethodChannel channel;
  private Context context;
  private StoneTech stoneTech;

  public StoneTechFlutterPlugin() {}

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
    channel = new MethodChannel(binding.getBinaryMessenger(), CHANNEL_NAME);
    //Get context to application
    context = binding.getApplicationContext();
    channel.setMethodCallHandler(this);
    //Create instance to Stone Smart class
    stoneTech = new StoneTech(context, channel);
  }

@Override
public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    try {
        if (call.method.startsWith("payment")) {
            stoneTech.initPayment(call, result);
        } else {
            result.notImplemented();
        }
    } catch (Exception e) {
        // Captura qualquer Exception e envia de volta para o Flutter
        result.error("ERROR", e.getMessage(), null);
    }
}


  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    //Dispose plugin
    channel.setMethodCallHandler(null);
    channel = null;
    stoneTech.dispose();
    stoneTech = null;
  }
}
