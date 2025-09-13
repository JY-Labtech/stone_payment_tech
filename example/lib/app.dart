import 'package:flutter/material.dart';
import 'package:stone_payment_tech_example/home_page.dart';

class AppWidget extends StatelessWidget {
  const AppWidget({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: "Stone Payment Tech",
      theme: ThemeData(primaryColor: Colors.deepOrange, primarySwatch: Colors.deepOrange),
      home: const HomePage(),
    );
  }
}
