import 'package:flutter/material.dart';

class StoneStreamStyle {
  final Color backgroundColor;
  final Color textColor;
  final Color buttonColor;
  final Color buttonTextColor;
  final IconData? icon;
  final Color iconColor;
  final Color textAlertColor;

  StoneStreamStyle({
    this.backgroundColor = Colors.white,
    this.textColor = Colors.black,
    this.buttonColor = Colors.blue,
    this.buttonTextColor = Colors.white,
    this.icon,
    this.iconColor = Colors.blue,
    this.textAlertColor = Colors.red,
  });
}
