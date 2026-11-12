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

import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

from coverage_summary import MODULES, build_summary


class CoverageSummaryTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.directory = Path(self.temporary.name)
        for module in MODULES:
            self.write_report(module)

    def write_report(self, module, covered=1, missed=1, branches=0, missed_branches=0):
        report = self.directory / module / "target/site/jacoco/jacoco.xml"
        report.parent.mkdir(parents=True, exist_ok=True)
        report.write_text(
            f'<report><package><counter type="LINE" covered="999" missed="0"/>'
            f'</package><counter type="LINE" covered="{covered}" missed="{missed}"/>'
            f'<counter type="BRANCH" covered="{branches}" missed="{missed_branches}"/>'
            f'</report>',
            encoding="utf-8",
        )
        return report

    def test_totals_use_only_root_counters_and_are_weighted(self):
        self.write_report(MODULES[0], covered=97, missed=1, branches=1, missed_branches=3)
        self.write_report(MODULES[1], branches=3, missed_branches=1)
        summary, errors = build_summary(self.directory)
        self.assertEqual([], errors)
        self.assertIn("| **Total** | **91.38% (106/116)** | **50.00% (4/8)** |", summary)

    def test_zero_branches_are_not_reported_as_full_coverage(self):
        summary, errors = build_summary(self.directory)
        self.assertEqual([], errors)
        self.assertIn(f"| {MODULES[0]} | 50.00% (1/2) | N/A |", summary)

    def test_missing_report_marks_total_incomplete(self):
        self.write_report(MODULES[0]).unlink()
        summary, errors = build_summary(self.directory)
        self.assertEqual(1, len(errors))
        self.assertIn("Report unavailable", summary)
        self.assertIn("**Total (incomplete)**", summary)

    def test_jacoco_omits_branch_counter_when_there_are_no_branches(self):
        self.write_report(MODULES[0]).write_text(
            '<report><counter type="LINE" covered="1" missed="0"/></report>',
            encoding="utf-8",
        )
        summary, errors = build_summary(self.directory)
        self.assertEqual([], errors)
        self.assertIn(f"| {MODULES[0]} | 100.00% (1/1) | N/A |", summary)

    def test_invalid_reports_are_explicit_errors(self):
        for xml in (
            "<report>",
            "<report/>",
            '<other><counter type="LINE" covered="1" missed="0"/></other>',
            '<report><counter type="LINE" covered="-1" missed="0"/></report>',
            '<report><counter type="LINE" covered="unknown" missed="0"/></report>',
            '<report><counter type="LINE" covered="1"/></report>',
        ):
            with self.subTest(xml=xml):
                self.write_report(MODULES[0]).write_text(xml, encoding="utf-8")
                summary, errors = build_summary(self.directory)
                self.assertEqual(1, len(errors))
                self.assertIn("**Total (incomplete)**", summary)

    def test_cli_writes_summary_and_fails_for_missing_reports(self):
        output = self.directory / "summary.md"
        script = Path(__file__).with_name("coverage_summary.py")
        for missing in (False, True):
            with self.subTest(missing=missing):
                if missing:
                    self.write_report(MODULES[0]).unlink()
                result = subprocess.run(
                    [sys.executable, str(script), str(self.directory)],
                    env={**os.environ, "GITHUB_STEP_SUMMARY": str(output)},
                    capture_output=True,
                    text=True,
                )
                self.assertEqual(1 if missing else 0, result.returncode, result.stderr)
                self.assertIn("## Test coverage", output.read_text(encoding="utf-8"))
                if missing:
                    self.assertIn(MODULES[0], result.stderr)


if __name__ == "__main__":
    unittest.main()
