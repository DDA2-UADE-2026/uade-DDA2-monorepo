"""Export JUnit outcomes only, deliberately omitting stdout, requests and failure text."""
import datetime
import json
import os
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

source, destination = map(Path, sys.argv[1:3])
files = sorted(source.rglob('*.xml'))
if not files:
    raise SystemExit('No JUnit XML files found; evidence cannot be generated')
suites = []
for file in files:
    root = ET.parse(file).getroot()
    for suite in root.iter('testsuite'):
        cases = suite.findall('testcase')
        if not cases:
            continue
        suites.append({
            'name': suite.get('name'),
            'tests': int(suite.get('tests', len(cases))),
            'failures': int(suite.get('failures', 0)),
            'errors': int(suite.get('errors', 0)),
            'skipped': int(suite.get('skipped', 0)),
            'cases': [{'name': c.get('name'), 'result':
                'FAIL' if c.find('failure') is not None or c.find('error') is not None else
                'SKIPPED' if c.find('skipped') is not None else 'PASS'} for c in cases],
        })
if not suites:
    raise SystemExit('No test cases found; evidence cannot be generated')
report = {
    'semantics': 'JUnit outcomes as reported; expected-failure tests may appear as PASS. Skipped cases are not executed.',
    'generatedUtc': datetime.datetime.now(datetime.timezone.utc).isoformat(),
    'commit': os.getenv('GITHUB_SHA'),
    'runId': os.getenv('GITHUB_RUN_ID'),
    'attempt': os.getenv('GITHUB_RUN_ATTEMPT'),
    'environment': {k: os.getenv(k) for k in ('RUNNER_OS', 'RUNNER_ARCH', 'ImageOS', 'ImageVersion')},
    'totals': {key: sum(s[key] for s in suites) for key in ('tests', 'failures', 'errors', 'skipped')},
    'suites': suites,
}
destination.parent.mkdir(parents=True, exist_ok=True)
destination.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
print(json.dumps(report['totals']))
