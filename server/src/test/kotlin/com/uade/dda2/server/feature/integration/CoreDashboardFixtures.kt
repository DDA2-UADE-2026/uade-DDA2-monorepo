package com.uade.dda2.server.feature.integration

internal object CoreDashboardFixtures {
    fun json(module: String = "desarrollo-social", windowHours: Int = 24) = """{
    "module":"$module","windowHours":$windowHours,"generatedAt":"2026-11-02T13:23:16Z",
    "published":{"total":8,"inWindow":2,"topTypes":[{"eventType":"probe","total":2}]},
    "received":{"byStatus":{"DELIVERED":3},"total":3,"delivered":3,"pendingRetry":0,"dead":0},
    "deadLetters":{"open":0,"byReason":{}},
    "subscriptions":{"total":1,"active":1,"eventTypes":["probe"]},
    "publications":{"declared":["probe"]},
    "volumeByHour":[{"hour":"2026-11-02T13:00:00Z","total":2,"rejected":0}],
    "processing":{"avgMs":3.25,"maxMs":8.0},"futureMetric":{"ignored":true}
}"""
}
