load(
    "@com_googlesource_gerrit_bazlets//:gerrit_plugin.bzl",
    "gerrit_plugin",
    "gerrit_plugin_tests",
)
load("@rules_java//java:defs.bzl", "java_library")
load("@rules_shell//shell:sh_test.bzl", "sh_test")

PLUGIN = "multi-site"

gerrit_plugin(
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: multi-site",
        "Gerrit-Module: com.gerritforge.gerrit.plugins.multisite.PluginModule",
        "Gerrit-HttpModule: com.gerritforge.gerrit.plugins.multisite.http.HttpModule",
        "Implementation-Title: multi-site plugin",
        "Implementation-URL: https://github.com/GerritForge/multi-site",
    ],
    plugin = PLUGIN,
    resources = glob(["src/main/resources/**/*"]),
    deps = [
        ":events-broker-neverlink",
        ":global-refdb-neverlink",
        ":pull-replication-neverlink",
        ":replication-neverlink",
    ],
)

gerrit_plugin_tests(
    srcs = glob(["src/test/java/**/*.java"]),
    plugin = PLUGIN,
    resources = glob(["src/test/resources/**/*"]),
    tags = ["local"],
    deps = [
        "//plugins/events-broker",
        "//plugins/global-refdb",
        "//plugins/pull-replication",
        "//plugins/replication",
    ],
)

java_library(
    name = "replication-neverlink",
    neverlink = 1,
    exports = ["//plugins/replication"],
)

java_library(
    name = "pull-replication-neverlink",
    neverlink = 1,
    exports = ["//plugins/pull-replication"],
)

java_library(
    name = "events-broker-neverlink",
    neverlink = 1,
    exports = ["//plugins/events-broker"],
)

java_library(
    name = "global-refdb-neverlink",
    neverlink = 1,
    exports = ["//plugins/global-refdb"],
)

filegroup(
    name = "e2e_multi_site_test_dir",
    srcs = [
        "e2e-tests",
    ],
)

filegroup(
    name = "e2e_multi_site_setup_local_env_dir",
    srcs = [
        "setup_local_env",
    ],
)

sh_test(
    name = "e2e_multi_site_tests",
    srcs = [
        "e2e-tests/test.sh",
    ],
    args = [
        "--multisite-lib-file $(location //plugins/multi-site)",
        "--global-refdb-lib-file $(location //plugins/global-refdb)",
        "--events-broker-lib-file $(location //plugins/events-broker)",
        "--replication-lib-file $(location //plugins/replication)",
        "--healthcheck-interval 5s",
        "--healthcheck-timeout 10s",
        "--healthcheck-retries 30",
        "--location '$(location //plugins/multi-site:e2e_multi_site_test_dir)'",
        "--local-env '$(location //plugins/multi-site:e2e_multi_site_setup_local_env_dir)'",
        "--gerrit-war '$(location //:gerrit.war)'",
    ],
    data = [
        "//:gerrit.war",
        "//plugins/events-broker",
        "//plugins/global-refdb",
        "//plugins/multi-site",
        "//plugins/multi-site:e2e_multi_site_setup_local_env_dir",
        "//plugins/multi-site:e2e_multi_site_test_dir",
        "//plugins/replication",
    ] + glob(["setup_local_env/**/*"]) + glob(["e2e-tests/**/*"]),
    tags = [
        "e2e-multi-site",
    ],
)
