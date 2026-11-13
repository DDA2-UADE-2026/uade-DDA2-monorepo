import fs from 'node:fs';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import { expect } from '../../client/node_modules/chai/index.js';

const root = fileURLToPath(new URL('../../', import.meta.url));
const collection = JSON.parse(fs.readFileSync(path.join(root, 'server/docs/postman/program-feature.postman_collection.json'), 'utf8'));
function flatten(items) { return items.flatMap(i => i.item ? flatten(i.item) : [i]); }
const requests = flatten(collection.item);
const directory = path.join(root, 'server/build/qa-postman');
const cases = [];
for (const file of fs.readdirSync(directory).filter(x => x.endsWith('.json')).sort()) {
  const evidence = JSON.parse(fs.readFileSync(path.join(directory, file), 'utf8'));
  if (['visit-future', 'visit-repeat'].includes(evidence.id)) {
    cases.push({ id: evidence.id, status: evidence.status, code: evidence.body.code,
      result: evidence.status === 409 ? 'PASS' : 'FAIL', source: 'negative HTTP assertion (not Postman script)' });
    continue;
  }
  const item = requests.find(x => x.name === evidence.requestName);
  if (!item) throw new Error(`Missing request ${evidence.requestName}`);
  const pm = {
    expect,
    response: {code: evidence.status, json: () => evidence.body,
      to: {have: {status: expected => expect(evidence.status).equal(expected)}}},
    test: (name, action) => {
      try { action(); cases.push({id:evidence.id, name, result:'PASS'}); }
      catch (error) {cases.push({id:evidence.id, name, result:'FAIL', error:error.message});}
    },
  };
  for (const event of item.event ?? []) {
    if (event.listen === 'test') vm.runInNewContext(event.script.exec.join('\n'), {pm}, {timeout:1000});
  }
}
const report = { environment:'MockMvc + H2 synthetic fixture responses; actual collection test scripts; not Newman/network E2E', cases };
fs.writeFileSync(path.join(root,'notes/qa-postman-results.json'), JSON.stringify(report,null,2));
console.log(JSON.stringify(report,null,2));
process.exitCode = cases.some(x => x.result === 'FAIL') ? 1 : 0;
