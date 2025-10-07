import 'package:flutter/material.dart';

class StoneSnackHelper {
  static void showSnackInformation(
    String message,
    Color color, {
    required BuildContext context,
    int seconds = 3,
  }) {
    FocusScope.of(context).requestFocus(
      FocusNode(),
    );
    ScaffoldMessenger.of(context).removeCurrentSnackBar();
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          message,
          style: const TextStyle(color: Colors.white),
        ),
        backgroundColor: color,
        duration: Duration(
          seconds: seconds,
        ),
      ),
    );
  }

  static void showSnackDark(
    String message,
    Color color, {
    required BuildContext context,
    int seconds = 3,
  }) {
    FocusScope.of(context).requestFocus(
      FocusNode(),
    );
    ScaffoldMessenger.of(context).removeCurrentSnackBar();
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          message,
          style: const TextStyle(color: Colors.black),
        ),
        backgroundColor: color,
        duration: Duration(
          seconds: seconds,
        ),
      ),
    );
  }

  static void removeSnackBar(BuildContext context) {
    ScaffoldMessenger.of(context).removeCurrentSnackBar();
  }

  static void showSnackException(
    String message,
    Color color, {
    required BuildContext context,
    int seconds = 5,
  }) {
    FocusScope.of(context).requestFocus(
      FocusNode(),
    );
    ScaffoldMessenger.of(context).removeCurrentSnackBar();
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          message,
          style: const TextStyle(color: Colors.white),
        ),
        backgroundColor: color,
        duration: Duration(
          seconds: seconds,
        ),
      ),
    );
  }
}
