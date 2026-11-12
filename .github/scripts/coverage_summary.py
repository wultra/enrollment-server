# PowerAuth Enrollment Server
# Copyright (C) 2026 Wultra s.r.o.
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as published
# by the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

"""Publish production-module JaCoCo counters to the GitHub Actions summary."""

import os
from pathlib import Path
import sys
import xml.etree.ElementTree as ET


MODULES = (
    "mtoken-model",
    "enrollment-server",
    "enrollment-server-onboarding",
    "enrollment-server-onboarding-api-model",
    "enrollment-server-onboarding-common",
    "enrollment-server-onboarding-domain-model",
    "enrollment-server-onboarding-provider-innovatrics",
    "enrollment-server-onboarding-provider-iproov",
    "enrollment-server-onboarding-provider-microblink",
    "enrollment-server-onboarding-provider-zenid",
)
COUNTERS = ("LINE", "BRANCH")


def read_counters(report):
    root = ET.parse(report).getroot()
    if root.tag != "report":
        raise ValueError(f"{report}: expected a JaCoCo report")
    result = {}
    for kind in COUNTERS:
        counter = root.find(f"./counter[@type='{kind}']")
        if counter is None:
            if kind == "BRANCH":
                result[kind] = (0, 0)
                continue
            raise ValueError(f"{report}: missing {kind} counter")
        covered, missed = int(counter.attrib["covered"]), int(counter.attrib["missed"])
        if covered < 0 or missed < 0:
            raise ValueError(f"{report}: negative {kind} counter")
        result[kind] = (covered, missed)
    return result


def percentage(counter):
    covered, missed = counter
    total = covered + missed
    return f"{100 * covered / total:.2f}% ({covered}/{total})" if total else "N/A"


def build_summary(directory):
    lines = [
        "## Test coverage",
        "",
        "| Module | Lines | Branches |",
        "| --- | ---: | ---: |",
    ]
    totals = {kind: (0, 0) for kind in COUNTERS}
    errors = []
    for module in MODULES:
        report = directory / module / "target/site/jacoco/jacoco.xml"
        try:
            counters = read_counters(report)
        except (OSError, ET.ParseError, ValueError, KeyError) as error:
            errors.append(f"{module}: {error}")
            lines.append(f"| {module} | Report unavailable | Report unavailable |")
            continue
        lines.append(
            f"| {module} | {percentage(counters['LINE'])} "
            f"| {percentage(counters['BRANCH'])} |"
        )
        for kind in COUNTERS:
            totals[kind] = tuple(a + b for a, b in zip(totals[kind], counters[kind]))
    label = "Total (incomplete)" if errors else "Total"
    lines.extend([
        f"| **{label}** | **{percentage(totals['LINE'])}** "
        f"| **{percentage(totals['BRANCH'])}** |",
        "",
        "Totals use summed covered/total counters, not averaged module percentages.",
        "Download the Maven test artifact for the HTML reports.",
        "",
    ])
    if errors:
        lines.append("**Coverage is incomplete: one or more reports are missing or invalid.**")
    return "\n".join(lines) + "\n", errors


def main():
    summary, errors = build_summary(Path(sys.argv[1]))
    with Path(os.environ["GITHUB_STEP_SUMMARY"]).open("a", encoding="utf-8") as output:
        output.write(summary)
    for error in errors:
        print(error, file=sys.stderr)
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
