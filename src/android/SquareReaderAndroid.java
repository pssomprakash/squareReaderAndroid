package com.om.squareReaderAndroid;

import android.util.Log;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.squareup.sdk.reader.ReaderSdk;

import com.squareup.sdk.reader.authorization.AuthorizationManager;
import com.squareup.sdk.reader.authorization.AuthorizationState;
import com.squareup.sdk.reader.authorization.AuthorizeErrorCode;
import com.squareup.sdk.reader.authorization.DeauthorizeErrorCode;
import com.squareup.sdk.reader.authorization.Location;
import com.squareup.sdk.reader.core.CallbackReference;
import com.squareup.sdk.reader.core.Result;
import com.squareup.sdk.reader.core.ResultError;

import com.squareup.sdk.reader.checkout.AdditionalPaymentType;
import com.squareup.sdk.reader.checkout.CheckoutErrorCode;
import com.squareup.sdk.reader.checkout.CheckoutManager;
import com.squareup.sdk.reader.checkout.CheckoutParameters;
import com.squareup.sdk.reader.checkout.CheckoutResult;
import com.squareup.sdk.reader.checkout.CurrencyCode;
import com.squareup.sdk.reader.checkout.Money;
import com.squareup.sdk.reader.hardware.ReaderManager;
import com.squareup.sdk.reader.hardware.ReaderSettingsErrorCode;

public class SquareReaderAndroid extends CordovaPlugin {
  private static final String LOG_TAG = "square-reader-plugin";

  // Authorization
  private AuthorizationManager authManager;
  private CallbackReference authorizeCodeCallbackRef;
  private CallbackContext authorizeCodeCallbackContext;
  // Deauthorization
  private CallbackReference deauthorizeCallbackRef;
  private CallbackContext deauthorizeCallbackContext;

  // Checkout
  private CheckoutManager checkoutManager;
  private CallbackReference checkoutCallbackRef;
  private CallbackContext checkoutCallbackContext;

  // Reader
  private ReaderManager readerSettingsManager;
  private CallbackReference readerSettingsCallbackRef;
  private CallbackContext readerSettingsCallbackContext;


  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

    if ("authorizeCode".equals(action)) {
      String authorizationCode = args.getString(0);
      this.authorizeCode(authorizationCode, callbackContext, this);

    } else if ("deauthorize".equals(action)) {
      this.deauthorize(callbackContext, this);

    } else if ("isAuthorized".equals(action)) {
      this.isAuthorized(callbackContext);

    } else if ("canDeauthorize".equals(action)) {
      this.canDeauthorize(callbackContext);

    } else if ("startCheckout".equals(action)) {
      int amountInCents = args.getInt(0);
      String note = args.getString(1);
      CurrencyCode currencyCode = CurrencyCode.valueOf(args.getString(1));
      this.startCheckout(new Money(amountInCents, currencyCode), note, callbackContext);

    } else if ("openReaderSettings".equals(action)) {
      this.openReaderSettings(callbackContext);

    } else {
      return false;
    }

    return true;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    this.clearCallbackReferences();
  }

  /* Exposed API */
  private void authorizeCode(String code, CallbackContext callbackContext, SquareReaderAndroid self) {
    // Square SDK's authorize() needs to be run on the main (UI) thread
    this.cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        self.initAuthorizationManagerIfNeeded();
        self.authorizeCodeCallbackContext = callbackContext;
        self.authManager.authorize(code);
      }
    });
  }

  private void deauthorize(CallbackContext callbackContext, SquareReaderAndroid self) {
    this.cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        self.initAuthorizationManagerIfNeeded();
        self.deauthorizeCallbackContext = callbackContext;

        if (self.canDeauthorize()) {
          self.authManager.deauthorize();
        } else {
          callbackContext.error("Error: Cannot deauthorize at this time! (Tip: You can use canDeauthorize() to check beforehand.)");
        }
      }
    });
  }

  private void canDeauthorize(CallbackContext callbackContext) {
    callbackContext.success(this.canDeauthorize() ? 1 : 0);
  }

  private void isAuthorized(CallbackContext callbackContext) {
    callbackContext.success(this.isAuthorized() ? 1 : 0);
  }

  private void startCheckout(Money amountMoney, String note, CallbackContext callbackContext) {
    if (this.isAuthorized()) {
      this.goToCheckoutActivity(amountMoney, note, callbackContext, this);
    } else {
      this.checkoutCallbackContext.error("Error: Not authorized to start checkout! (Tip: You can use isAuthorized() to check beforehand)");
    }
  }

  private void openReaderSettings(CallbackContext callbackContext) {
    this.goToReaderSettingsActivity(callbackContext, this);
  }


  /* Authorization/Deauthorization */
  private void onAuthorizeResult(Result<Location, ResultError<AuthorizeErrorCode>> result) {
    if (result.isSuccess()) {
      this.authorizeCodeCallbackContext.success("success: authorization success!");
    } else {
      ResultError<AuthorizeErrorCode> error = result.getError();

      switch (error.getCode()) {
        case NO_NETWORK:
          this.authorizeCodeCallbackContext.error(error.getDebugCode() + ", " + error.getDebugMessage());
          break;
        case USAGE_ERROR:
          this.authorizeCodeCallbackContext.error(error.getDebugCode() + ", " + error.getDebugMessage());
          break;
        default:
          this.authorizeCodeCallbackContext.error(error.getDebugCode() + ", " + error.getDebugMessage());
      }
    }
  }

  private void onDeauthorizeResult(Result<Void, ResultError<DeauthorizeErrorCode>> result) {
    if (result.isSuccess()) {
      this.deauthorizeCallbackContext.success("deauthorization successful!");
    } else {
      ResultError<DeauthorizeErrorCode> error = result.getError();

      switch (error.getCode()) {
        case USAGE_ERROR:
          this.deauthorizeCallbackContext.error(error.getDebugCode() + ", " + error.getDebugMessage());
          break;
        default:
          this.deauthorizeCallbackContext.error(error.getDebugCode() + ", " + error.getDebugMessage());
      }
    }
  }

  private boolean canDeauthorize() {
    this.initAuthorizationManagerIfNeeded();
    AuthorizationState authState = this.authManager.getAuthorizationState();
    return authState.canDeauthorize();
  }

  private boolean isAuthorized() {
    this.initAuthorizationManagerIfNeeded();
    AuthorizationState authState = this.authManager.getAuthorizationState();
    return authState.isAuthorized();
  }

  private void initAuthorizationManagerIfNeeded() {
    if (this.authManager == null) {
      this.authManager = ReaderSdk.authorizationManager();
      this.authorizeCodeCallbackRef = this.authManager.addAuthorizeCallback(this::onAuthorizeResult);
      this.deauthorizeCallbackRef = this.authManager.addDeauthorizeCallback(this::onDeauthorizeResult);
    }
  }

  /* Checkout */
  private void goToCheckoutActivity(Money amountMoney, String note, CallbackContext callbackContext, SquareReaderAndroid self) {
    this.cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        self.initCheckoutManagerIfNeeded();
        self.checkoutCallbackContext = callbackContext;

        CheckoutParameters.Builder parametersBuilder = CheckoutParameters.newBuilder(amountMoney);
        parametersBuilder.skipReceipt(true);
        parametersBuilder.note(note);
        // parametersBuilder.additionalPaymentTypes(AdditionalPaymentType.CASH);
        parametersBuilder.additionalPaymentTypes(AdditionalPaymentType.CASH, AdditionalPaymentType.MANUAL_CARD_ENTRY, AdditionalPaymentType.OTHER);

        self.checkoutManager.startCheckoutActivity(self.cordova.getActivity(), parametersBuilder.build());
      }
    });
  }

  private void onCheckoutResult(Result<CheckoutResult, ResultError<CheckoutErrorCode>> result) {
    if (result.isSuccess()) {
      CheckoutResult checkoutResult = result.getSuccessValue();
      JSONObject payload = new JSONObject();

      Log.d(LOG_TAG, checkoutResult.toString());

      try {
        // payload.put("transactionId", checkoutResult.getTransactionId());
        payload.put("transactionID", (checkoutResult.getTransactionId() != null ? checkoutResult.getTransactionId() : ""));
        payload.put("amountCollected", checkoutResult.getTotalMoney().getAmount());
        payload.put("transactionClientID", checkoutResult.getTransactionClientId());
        payload.put("locationID", checkoutResult.getLocationId());
        this.checkoutCallbackContext.success(payload);
      } catch (JSONException e) {
        Log.d(LOG_TAG, e.getMessage());
        this.checkoutCallbackContext.error("error: couldn't parse checkout result to json");
      }
    } else {
      ResultError<CheckoutErrorCode> error = result.getError();

      switch (error.getCode()) {
        case SDK_NOT_AUTHORIZED:
          this.checkoutCallbackContext.error(error.getDebugCode() + ", " + error.getDebugMessage());
          break;
        case CANCELED:
          this.checkoutCallbackContext.error("error: checkout cancelled!");
          break;
        case USAGE_ERROR:
          this.checkoutCallbackContext.error(error.getDebugCode() + ", " + error.getDebugMessage());
          break;
        default:
          this.checkoutCallbackContext.error(error.getDebugCode() + ", " + error.getDebugMessage());
      }
    }
  }

  private void initCheckoutManagerIfNeeded() {
    if (this.checkoutManager == null) {
      this.checkoutManager = ReaderSdk.checkoutManager();
      this.checkoutCallbackRef = this.checkoutManager.addCheckoutActivityCallback(this::onCheckoutResult);
    }
  }

  /* Reader */
  private void goToReaderSettingsActivity(CallbackContext callbackContext, SquareReaderAndroid self) {
    this.cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        self.initReaderManagerIfNeeded();
        self.readerSettingsCallbackContext = callbackContext;

        self.readerSettingsManager.startReaderSettingsActivity(self.cordova.getActivity());
      }
    });
  }

  private void onReaderSettingsResult(Result<Void, ResultError<ReaderSettingsErrorCode>> result) {
    if (result.isError()) {
      ResultError<ReaderSettingsErrorCode> error = result.getError();
      switch (error.getCode()) {
        case SDK_NOT_AUTHORIZED:
          this.readerSettingsCallbackContext.error(error.getDebugCode() + ", " + error.getDebugMessage());
          break;
        case USAGE_ERROR:
          this.readerSettingsCallbackContext.error(error.getDebugCode() + ", " + error.getDebugMessage());
          break;
      }
    }
  }

  private void initReaderManagerIfNeeded() {
    if (this.readerSettingsManager == null) {
      this.readerSettingsManager = ReaderSdk.readerManager();
      this.readerSettingsCallbackRef = this.readerSettingsManager.addReaderSettingsActivityCallback(this::onReaderSettingsResult);
    }
  }

  /* Misc */
  private void clearCallbackReferences() {
    if (this.authorizeCodeCallbackRef != null) {
      this.authorizeCodeCallbackRef.clear();
    }
    if (this.deauthorizeCallbackRef != null) {
      this.deauthorizeCallbackRef.clear();
    }
    if (this.checkoutCallbackRef != null) {
      this.checkoutCallbackRef.clear();
    }
    if (this.readerSettingsCallbackRef != null) {
      this.readerSettingsCallbackRef.clear();
    }
  }

  /* Static */
  private static void log(String message) {
    Log.d(LOG_TAG, message);
  }

}
