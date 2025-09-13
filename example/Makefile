ifeq (run-flavor,$(firstword $(MAKECMDGOALS)))
  RUN_ARGS := $(wordlist 2,$(words $(MAKECMDGOALS)),$(MAKECMDGOALS))
  $(eval $(RUN_ARGS):;@:)
endif


ifeq (build-apk,$(firstword $(MAKECMDGOALS)))
  RUN_ARGS := $(wordlist 2,$(words $(MAKECMDGOALS)),$(MAKECMDGOALS))
  $(eval $(RUN_ARGS):;@:)
endif

ifeq (build-ipa,$(firstword $(MAKECMDGOALS)))
  RUN_ARGS := $(wordlist 2,$(words $(MAKECMDGOALS)),$(MAKECMDGOALS))
  $(eval $(RUN_ARGS):;@:)
endif


ifeq (build-windows,$(firstword $(MAKECMDGOALS)))
  RUN_ARGS := $(wordlist 2,$(words $(MAKECMDGOALS)),$(MAKECMDGOALS))
  $(eval $(RUN_ARGS):;@:)
endif

ifeq (build-appbundle,$(firstword $(MAKECMDGOALS)))
  RUN_ARGS := $(wordlist 2,$(words $(MAKECMDGOALS)),$(MAKECMDGOALS))
  $(eval $(RUN_ARGS):;@:)
endif

ifeq (build-ios,$(firstword $(MAKECMDGOALS)))
  RUN_ARGS := $(wordlist 2,$(words $(MAKECMDGOALS)),$(MAKECMDGOALS))
  $(eval $(RUN_ARGS):;@:)
endif

ifeq (build-web,$(firstword $(MAKECMDGOALS)))
  RUN_ARGS := $(wordlist 2,$(words $(MAKECMDGOALS)),$(MAKECMDGOALS))
  $(eval $(RUN_ARGS):;@:)
endif


.PHONY=help

help:  ## This help
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST) | sort

###
# Run section
###
clean: ## Clean our project build tmp files
	fvm flutter clean && fvm flutter pub get
run: ## Run any app with flavor name. e.g. make run base
	fvm flutter run --release
xcode: ## Open xcode project
	open -a Xcode.app ios/
run-ios: ## Open xcode from build run
	open ios/Runner.xcworkspace

###
# Build section
###
open-build: ## Open build folder
	xdg-open build/app/outputs/flutter-apk

build-appbundle: ## Build an APK of any app with flavor name. e.g. make build base
	fvm flutter build appbundle --flavor ${RUN_ARGS} -t lib/main.dart
build-ios: ## Build an APK of any app with flavor name. e.g. make build base
	fvm flutter build ios --release
run-flavor: ## Run any app with flavor name. e.g. make run base
	fvm flutter run --flavor $(RUN_ARGS) -t lib/main.dart

build-apk: ## Build an APK of any app with flavor name. e.g. make build base
	fvm flutter build apk --flavor ${RUN_ARGS} -t lib/main.dart
build-ipa: ## Build an APK of any app with flavor name. e.g. make build base
	fvm flutter build ipa --release --flavor ${RUN_ARGS} -t lib/main.dart
build-windows: ## Build an APK of any app with flavor name. e.g. make build base
	fvm flutter build windows --flavor ${RUN_ARGS} -t lib/main.dart

###
# Dependencies section
###
dependencies: ## Install all dependencies

	fvm flutter pub get


###
# Tests section
###
test: ## Run all tests
	fvm flutter test
